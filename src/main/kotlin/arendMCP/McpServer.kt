package org.example.arendMCP

import io.modelcontextprotocol.kotlin.sdk.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.example.arendClient.ArendClientImpl


fun main() {
    val server: Server = createServer()
    val stdioServerTransport = StdioServerTransport(
        System.`in`.asSource().buffered(),
        System.out.asSink().buffered()
    )
    runBlocking {
        val job = Job()
        server.onClose { job.complete() }
        server.connect(stdioServerTransport)
        job.join()
    }
}

fun createServer(): Server {
    val info = Implementation(
        "arend_mcp_server",
        "1.3.0"
    )
    val options = ServerOptions(
        capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(true))
    )
    val server = Server(info, options)

    val arendClient = ArendClientImpl()

    for (tool in availableTools()) {
        server.addTool(
            name = tool.name,
            description = tool.description,
            Tool.Input(tool.inputSchema)
        ) { input ->
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
    if (arguments.keys.size == 1){
        return arguments.getValue("libraryPath").jsonPrimitive.content
    }
    val namesParameter = arguments.keys.filter { it != "libraryPath" }.firstOrNull() ?: throw IllegalArgumentException()
    val paramValue = arguments.getValue(namesParameter)
    val libPath = arguments.getValue("libraryPath").jsonPrimitive.content
    
    // Handle both string and array parameters
    return if (paramValue is kotlinx.serialization.json.JsonArray) {
        buildRequestFromList(converter(paramValue.map { it.jsonPrimitive.content }), libPath)
    } else {
        // For string parameters like "query" in Proof_search, return "value|||libPath"
        paramValue.jsonPrimitive.content
    }
}

fun converter(x : Any) : List<String>{
    if (x is String) return listOf(x)
    if (x is List<*>) return x as List<String>
    throw IllegalArgumentException()
}

private data class LocalTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
)

private fun availableTools(): List<LocalTool> = listOf(
    LocalTool(
        name = "mcp_arend_Typecheck_definition",
        description = "Typechecks what you wrote in Arend, returns error messages separated by comma." +
                "You need to send it the full library path as a string and a list of paths of modules that you want to typecheck." +
                "For example if in project myProject you want to typecheck module myFile.ard you send the json {\"libraryName\":\"/Users/username/Dev/myProject\",\"modulePaths\":[\"myFile\"]}"
        ,
        inputSchema = buildJsonObject {
            putJsonObject("libraryPath") {
                put("type", "string")
            }
            putJsonObject("modulePaths") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
            }
        }
    ),
    LocalTool(
        name = "mcp_arend_Proof_search",
        description = "Triggers Arend proof search for the query you send.",
        inputSchema = buildJsonObject {
            putJsonObject("libraryPath") { put("type", "string") }
            putJsonObject("query") {
                put("type", "string")
            }
        }
    ),
    LocalTool(
        name = "mcp_arend_List_modules",
        description = "Lists all supported modules from the current project and its library dependencies. " +
                "Returns full module identifiers. You need to send it the full library path as a string. " +
                "For example: {\"libraryPath\":\"/Users/username/Dev/myProject\"}",
        inputSchema = buildJsonObject {
            putJsonObject("libraryPath") { put("type", "string") }
        }
    ),
    LocalTool(
        name = "mcp_arend_List_modules_content",
        description = "Lists simplified content of specified modules, showing function signatures without bodies. " +
                "You need to send it the full library path as a string and a list of module paths. " +
                "For example: {\"libraryPath\":\"/Users/username/Dev/myProject\",\"modulePaths\":[\"myFile\"]}",
        inputSchema = buildJsonObject {
            putJsonObject("libraryPath") {
                put("type", "string")
            }
            putJsonObject("modulePaths") {
                put("type", "array")
                putJsonObject("items") {
                    put("type", "string")
                }
            }
        }
    )
)