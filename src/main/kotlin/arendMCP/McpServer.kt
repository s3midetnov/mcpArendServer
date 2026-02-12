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
        "1.2.0"
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
                TextContent(arendClient.typecheck_definition(libPath,modulePaths))
            )
        )
    }

    val proofSearchInputSchema = Tool.Input(
        buildJsonObject {
            putJsonObject("libraryPath") {
                put("type", "string")
            }
            putJsonObject("proofSearchQuery") {
                put("type", "string")
            }
        }
    )

    server.addTool(
        name = "mcp_arend_Proof_search",
        description = "Triggers Arend proof search for the query you send." +
                " You need to send it the full library path as a string and the query as a string." +
                "The grammar of Proof Search queries is defined as follows:\n" +
                "\n" +
                "  query ::= (and_pattern '->')* and_pattern\n" +
                "  and_pattern ::= (app_pattern '\\and')*\n" +
                "  app_pattern app_pattern ::= atom_pattern+\n" +
                "  atom_pattern ::= '_' | (IDENTIFIER '.')* IDENTIFIER | '(' app_pattern ')'" +
                "For example, the query Foo -> Bar will produce the following results:\n" +
                "\n" +
                "\\func foo (f : Foo) : Bar -- matched\n" +
                "\\func bar :    Foo -> Bar -- matched\n" +
                "\\func baz :           Bar -- not matched",
        proofSearchInputSchema
    ){input ->
        val libPath = input.arguments["libraryPath"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing libraryPath")

        val query = input.arguments["proofSearchQuery"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing query")

        CallToolResult(
            listOf(
                TextContent(arendClient.proof_search(libPath, query))
            )
        )
    }

    val listModulesInputSchema = Tool.Input(
        buildJsonObject {
            putJsonObject("libraryPath") {
                put("type", "string")
            }
        }
    )

    server.addTool(
        name = "mcp_arend_List_modules",
        description = "Lists all supported modules from the current project and its library dependencies. " +
                "Returns full module identifiers. You need to send it the full library path as a string. " +
                "For example: {\"libraryPath\":\"/Users/username/Dev/myProject\"}"
        ,listModulesInputSchema
    ){input ->
        val libPath = input.arguments["libraryPath"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing libraryPath")
        CallToolResult(
            listOf(
                TextContent(arendClient.list_modules(libPath))
            )
        )
    }

    val listModulesContentInputSchema = Tool.Input(
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
        name = "mcp_arend_List_modules_content",
        description = "Lists all contents of the modules you send. " +
                "You need to send it the full library path as a string and a list of module identifiers." +
                "Full identifier format: libraryName:locationKind:modulePath"
        ,listModulesContentInputSchema
    ){input ->
        val libPath = input.arguments["libraryPath"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing libraryPath")

        val modulePathsArray = input.arguments["modulePaths"]?.jsonArray
            ?: throw IllegalArgumentException("Missing modulePaths")

        val modulePaths = modulePathsArray.map { it.jsonPrimitive.content }
        CallToolResult(
            listOf(
                TextContent(arendClient.list_modules_content(libPath,modulePaths))
            )
        )
    }


    return server
}