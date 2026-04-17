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

    override suspend fun callArendAction(argument: Any?, actionId: String): String {
        val START_PORT = 63342
        val END_PORT = 63352
        val ENDPOINT = "api/detachedService"

        val actionPayload = argument?.toString() ?: ""

        val requestTimeout = Duration.ofSeconds(60)

        for (port in START_PORT..END_PORT) {
            try {
                val encodedPayload = URLEncoder.encode(actionPayload, StandardCharsets.UTF_8.toString())
                val url = "http://localhost:$port/$ENDPOINT?type=$actionId&action=$encodedPayload"

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                if (response.statusCode() == 200) {
                    return response.body()
                } else if (response.statusCode() in listOf(404, 401, 403)) {
                    continue
                }

            } catch (e: java.net.ConnectException) {
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
