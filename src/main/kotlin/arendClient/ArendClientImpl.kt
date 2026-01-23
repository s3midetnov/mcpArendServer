package org.example.arendClient


import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Base64

class ArendClientImpl : ArendClient {
    override suspend fun typecheck_definition(definition: String): String {
        val fileWithAnswers = File("/Users/artem.semidetnov/Dev/mcpArendServer/src/main/kotlin/errorList.txt")
        fileWithAnswers.writeText("")

        triggerAction("typecheck")
        Thread.sleep(1000)

        val listWithErrors = fileWithAnswers.readLines()

        return listWithErrors.joinToString("\n")
    }

    fun triggerAction(actionId: String): Boolean {
        val START_PORT = 63342
        val END_PORT = 63352
        val ENDPOINT = "/api/detachedTypechecker" // Must match your plugin's handler path

        // Use the modern Java 11+ HttpClient
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build()

        for (port in START_PORT..END_PORT) {

            val url = "http://localhost:$port$ENDPOINT?actionId=$actionId"

            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build()

                // Send request
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                when (response.statusCode()) {
                    200 -> {
//                        println("✅ Success! Triggered action on port $port.")
//                        println("   Response: ${response.body()}")
                        return true
                    }
                    404 -> {
//                        println("❌ Found IntelliJ on port $port, but the plugin endpoint was not found.")
//                        println("   (Ensure your plugin is installed and enabled in this instance).")
                        // We found the IDE, so we stop scanning to avoid hitting other instances randomly.
                        continue
                    }
                    401, 403 -> {
//                        println("🔒 Found IntelliJ on port $port, but access was denied (Authorization required).")
                        continue
                    }
                    else -> {
                        // 500s or other errors
//                        println("⚠️ Connected to port $port but received error: ${response.statusCode()}")
                        return false
                    }
                }

            } catch (e: java.net.ConnectException) {
                // Port is closed, just continue to the next one
                continue
            } catch (e: Exception) {
//                println("⚠️ Unexpected error on port $port: ${e.message}")
            }
        }
        return false
    }
}