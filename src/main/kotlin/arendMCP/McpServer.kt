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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.buildJsonArray
import org.example.arendClient.ArendClient
import org.example.arendClient.ArendClientImpl
import java.io.File


fun main() {
    val delimiter = "%%"
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

//    val codeInputSchema = Tool.Input(
//        buildJsonObject {
//            put("type", JsonPrimitive("object"))
//            putJsonObject("properties") {
//                putJsonObject("libraryName") {
//                    put("type", JsonPrimitive("string"))
//                }
//                putJsonObject("modulePaths") {
//                    put("type", JsonPrimitive("array"))
//                    putJsonObject("items") {
//                        put("type", JsonPrimitive("string"))
//                    }
//                }
//            }
//            put("required", kotlinx.serialization.json.JsonArray(
//                listOf(JsonPrimitive("libraryName"), JsonPrimitive("modulePaths"))
//            ))
//        }
//    )
    val codeInputSchema = Tool.Input(
        buildJsonObject {
                putJsonObject("libraryName") {
                    put("type", "string")
                }
                putJsonObject("modulePaths") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "string")
                    }
                }

//            put("required", buildJsonArray {
//                add("libraryName")
//                add("modulePaths")
//            })
        }
    )

    server.addTool(
        name = "mcp_arend_Typecheck_definition",
        description = "Typechecks what you wrote in Arend, returns error messages separated by comma." +
                "You need to send it the library name as a string and a list of paths of modules that you want to typecheck." +
                "For example if in project myProject you want to typecheck module myFile.ard you send the json {\"libraryName\":\"myProject\",\"modulePaths\":[\"myFile\"]}",
        codeInputSchema
    )
    { input ->
        val libName = input.arguments["libraryName"]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Missing libraryName")

        // FIX 2: Handle modulePaths as a JsonArray, not a JsonPrimitive
        val modulePathsArray = input.arguments["modulePaths"]?.jsonArray
            ?: throw IllegalArgumentException("Missing modulePaths")

        // Convert JsonArray to a List<String> for your internal logic
        val modulePaths = modulePathsArray.map { it.jsonPrimitive.content }
//
//        val libName = input.arguments["libraryName"]!!.jsonPrimitive.content
//        val modulePaths = input.arguments["modulePaths"]!!.jsonPrimitive.content
        CallToolResult(
            listOf(
                TextContent(arendClient.typecheck_definition(modulePaths.fold("", { acc, s -> acc + s + "%%"}) + libName))
            )
        )
    }
    return server
}