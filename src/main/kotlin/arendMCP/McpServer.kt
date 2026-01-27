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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.example.arendClient.ArendClient
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
        "Arend_goal_typecheck_MCP",
        "1.1.0"
    )
    val options = ServerOptions(
        capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(true))
    )
    val server = Server(info, options)
    val arendClient: ArendClient = ArendClientImpl()
    val codeInputSchema = Tool.Input(
        buildJsonObject {
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

    server.addTool(
        name = "mcp_arend_Typecheck_definition",
        description = "Typechecks what you wrote in Arend, returns error messages separated by comma." +
                "You need to send it the full library path as a string and a list of paths of modules that you want to typecheck." +
                "For example if in project myProject you want to typecheck module myFile.ard you send the json {\"libraryName\":\"/Users/username/Dev/myProject\",\"modulePaths\":[\"myFile\"]}",
        codeInputSchema
    )
    { input ->
        val libPath = input.arguments["libraryPath"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing libraryPath")

        val modulePathsArray = input.arguments["modulePaths"]?.jsonArray
            ?: throw IllegalArgumentException("Missing modulePaths")

        val modulePaths = modulePathsArray.map { it.jsonPrimitive.content }

        CallToolResult(
            listOf(
                TextContent(arendClient.typecheck_definition(libPath,modulePaths.fold("", { acc, s -> "$acc$s%%" })))
            )
        )
    }
    return server
}