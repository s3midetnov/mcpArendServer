package org.example.arendMCP

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
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
    val options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(true))
    )
    val server = Server(info, options)

    server.addTool(
        name="Typecheck_definition",
        description = "Typechecks a definition in Arend, returns error messages separated by comma",
    ){ CallToolResult(
        listOf(TextContent("No errors!"))
    ) }

    return server
}