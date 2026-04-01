package org.example.arendMCP

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.*
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.concurrent.atomic.AtomicLong
import org.example.arendClient.ArendClientImpl


private val lastInteractionTime = AtomicLong(System.currentTimeMillis())
private val INACTIVITY_TIMEOUT_MS = 2 * 60 * 60 * 1000L // 2 hours

fun main() {
    val server: Server = createServer {
        lastInteractionTime.set(System.currentTimeMillis())
    }
    val stdioServerTransport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )
    runBlocking {
        val job = Job()
        server.onClose { 
            job.complete() 
        }

        launch {
            while (job.isActive) {
                delay(60000) // Check every minute
                if (System.currentTimeMillis() - lastInteractionTime.get() > INACTIVITY_TIMEOUT_MS) {
                    System.err.println("Inactivity timeout reached. Shutting down...")
                    job.complete()
                    break
                }
            }
        }

        server.connect(stdioServerTransport)
        job.join()
    }
}

fun createServer(onInteraction: () -> Unit): Server {
    val info = Implementation(
        "arend_mcp_server",
        "1.3.0"
    )
    val options = ServerOptions(
        capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(true))
    )
    val server = Server(info, options)

    val arendClient = ArendClientImpl()

    val tools = availableTools()
    System.err.println("Available tools:")
    for (tool in tools) {
        System.err.println("- ${tool.name}")
        server.addTool(
            name = tool.name,
            description = tool.description,
            Tool.Input(tool.inputSchema)
        ) { input ->
            onInteraction()
            val libPath = input.arguments["libraryPath"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("Missing libraryPath")

            // Extract arguments (excluding libraryPath) for formatPath
            val arguments = extractArguments(input.arguments)

            CallToolResult(
                listOf(
                    TextContent(
                        arendClient.callArendAction(libPath, arguments, tool.name)
                    )
                )
            )
        }
    }
    return server
}

fun buildRequestFromList(names : List<String>, libPath : String) = names.joinToString(separator = "|||")
fun extractArguments(arguments : JsonObject) : String {
    if (arguments.keys.isEmpty()) throw IllegalArgumentException()
    if (arguments.keys.size == 1 && arguments.containsKey("libraryPath")){
        return arguments.getValue("libraryPath").jsonPrimitive.content
    }
    val namesParameter = arguments.keys.filter { it != "libraryPath" }.firstOrNull() ?: throw IllegalArgumentException()
    val paramValue = arguments.getValue(namesParameter)
    
    // Handle both string and array parameters
    return if (paramValue is kotlinx.serialization.json.JsonArray) {
        val libPath = arguments["libraryPath"]?.jsonPrimitive?.content ?: ""
        buildRequestFromList(converter(paramValue.map { it.jsonPrimitive.content }), libPath)
    } else {
        // For string parameters like "query" in Proof_search, return "value"
        paramValue.jsonPrimitive.content
    }
}

fun converter(x : Any) : List<String>{
    if (x is String) return listOf(x)
    if (x is List<*>) return x as List<String>
    throw IllegalArgumentException()
}

@Serializable
private data class LocalTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)

@Serializable
private data class ToolList(
    val tools: List<LocalTool>
)

private fun availableTools(): List<LocalTool> {
    val inputStream = object {}.javaClass.classLoader.getResourceAsStream("mcp-tools.json")
        ?: return emptyList()
    
    val content = inputStream.bufferedReader().use { it.readText() }
    return try {
        val toolList = Json.decodeFromString<ToolList>(content)
        toolList.tools
    } catch (e: Exception) {
        e.printStackTrace(System.err)
        emptyList()
    }
}