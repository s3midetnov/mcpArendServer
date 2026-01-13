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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject
import org.example.arendClient.ArendClient
import org.example.arendClient.ArendClientImpl
import java.io.File


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
            putJsonObject("definition") {
                JsonPrimitive("string")
            }
        }
    )

    server.addTool(
        name = "Typecheck_definition",
        description = "Typechecks a definition in Arend, returns error messages separated by comma." +
                " You need to send it a full definition, not just the name of a function, but the whole body and all dependencies",
        codeInputSchema
    )
    { input ->
        val definition = input.arguments["definition"]!!.jsonPrimitive.content
        CallToolResult(
            listOf(
                TextContent(arendClient.typecheck_definition(definition))
            )
        )
    }

    val specQuerySchema = Tool.Input(
        buildJsonObject {
            putJsonObject("topic") {
                JsonPrimitive("string")
            }
        }
    )

    server.addTool(
        name = "Generate_specification",
        description = "Generates a specification for a given topic, returns the specification as a string. " +
                "Topics are strings from the following list: 'Case expressions', 'Core language constructs', 'Equality proofs'," +
                "'Identity type', 'Records and classes system', 'Propositions and proofs'." +
                "Send to the tool only one topic at a time, exactly as in the list above",
        specQuerySchema
    ) { input ->
        val topic = input.arguments["topic"]!!.jsonPrimitive.content
        val folderName = "languageSpec"
        val home = System.getProperty("user.home")
        
        // Try to find the folder relative to the current working directory first,
        // then fall back to a path relative to the user's home directory.
        val folder = File(folderName).let { 
            if (it.exists()) it else File(home, "Dev/mcpArendServer/$folderName")
        }

        val topicToFile = mapOf(
            "Case expressions" to "CaseExpressions",
            "Core language constructs" to "CoreLanguageConstructs",
            "Equality proofs" to "EqualityProof",
            "Identity type" to "IdentityType",
            "Records and classes system" to "RecordClassSystem",
            "Propositions and proofs" to "PropositionsAndProofs"
        )

        val fileName = topicToFile[topic]
        val content = if (fileName != null) {
            try {
                File(folder, fileName).readText(Charsets.UTF_8)
            } catch (e: Exception) {
                "Error reading topic '$topic' from path '${File(folder, fileName).absolutePath}': ${e.message}. " +
                "Current working directory: ${System.getProperty("user.dir")}"
            }
        } else {
            "Topic not recognized: $topic"
        }

        CallToolResult(
            listOf(
                TextContent(content)
            )
        )
    }

    server.addTool(
        name = "Standard_library_location",
        description = "Returns the path to the standard library.",
    ){input ->
        CallToolResult(
            listOf(
                TextContent(
                    arendClient.return_library_location()
                )
            )
        )
    }
    return server
}