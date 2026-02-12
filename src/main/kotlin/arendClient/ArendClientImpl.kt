package org.example.arendClient

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class ArendClientImpl : ArendClient {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    override suspend fun typecheck_definition(projectPath: String, modules: List<String>): String {
        val modulesFolded = modules.joinToString("%%")
        val payload = "$modulesFolded%%$projectPath"
        return sendRequest("mcp_arend_Typecheck_definition", payload)
}

    override suspend fun proof_search(projectPath: String, query: String): String {
        return sendRequest("mcp_arend_Proof_search", query)
    }

    override suspend fun list_modules(projectPath: String): String {
        return sendRequest("mcp_arend_List_modules", projectPath)
    }

    override suspend fun list_modules_content(projectPath: String, modules: List<String>): String {
        val modulesFolded = modules.joinToString("%%")
        val payload = "$modulesFolded%%$projectPath"
        return sendRequest("mcp_arend_List_modules_content", payload)
    }

    /**
     * Sends the request and waits for the server to reply with the result string.
     * No more file polling!
     */
    private fun sendRequest(actionType: String, actionPayload: String): String {
        val START_PORT = 63342
        val END_PORT = 63352
        val ENDPOINT = "api/detachedService"

        // Increase timeout because Typechecking might be slow (e.g., 60 seconds)
        val requestTimeout = Duration.ofSeconds(60)

        for (port in START_PORT..END_PORT) {
            try {
                val encodedPayload = URLEncoder.encode(actionPayload, StandardCharsets.UTF_8.toString())
                val url = "http://localhost:$port/$ENDPOINT?type=$actionType&action=$encodedPayload"

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    return response.body()
                } else if (response.statusCode() in listOf(404, 401, 403)) {
                    // Port exists but service not found or auth failed, try next port
                    continue
                }

            } catch (e: java.net.ConnectException) {
                // Port closed, try next
                continue
            } catch (e: java.net.http.HttpTimeoutException) {
                return "Error: Request timed out. The operation took longer than ${requestTimeout.seconds}s."
            } catch (e: Exception) {
                return "Error: ${e.message}"
            }
        }
        return "Error: Could not connect to IntelliJ Arend Plugin (Ports $START_PORT-$END_PORT checked)."
    }
}