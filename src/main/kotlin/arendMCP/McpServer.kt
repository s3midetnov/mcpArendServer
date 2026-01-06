package org.example.arendMCP

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        "1.0.0"
    )
    val options = ServerOptions(
        capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(true))
    )
    val server = Server(info, options)
    val arendClient : ArendClient = ArendClientImpl()

    val codeInputSchema = Tool.Input(
        buildJsonObject {
            putJsonObject("function"){
                JsonPrimitive("string")
            }
        }
    )

    server.addTool(
        name="Typecheck_definition",
        description = "Typechecks a definition in Arend, returns error messages separated by comma",
        codeInputSchema
    )
    { input ->
        val function = input.arguments["function"]!!.jsonPrimitive.content
        CallToolResult(
        listOf(
            TextContent(arendClient.typecheck_definition(function)))
        )
    }

    return server
}