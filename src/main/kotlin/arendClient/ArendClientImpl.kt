package org.example.arendClient

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.ranges.rangeTo

class ArendClientImpl : ArendClient{
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build()

    companion object {
        private const val DELIMITER = "|||"

        /**
         * Formats the path for the Arend IntelliJ plugin's DetachedHTTPService.
         * - If input is a single string: "string|||libPath"
         * - If input is a list of strings: "str1|||str2|||...str_last|||libPath"
         */
        fun formatPath(input: Any?, libPath: String): String {
            return when (input) {
                is String -> if (input.isEmpty()) libPath else "$input$DELIMITER$libPath"
                is List<*> -> {
                    val joined = input.filterIsInstance<String>().joinToString(DELIMITER)
                    if (joined.isEmpty()) libPath else "$joined$DELIMITER$libPath"
                }
                null -> libPath
                else -> throw IllegalArgumentException("Input must be a String or a List<String>")
            }
        }
    }

    override suspend fun callArendAction(libPath: String, argument: Any?, actionId: String) : String {
        val START_PORT = 63342
        val END_PORT = 63352
        val ENDPOINT = "api/detachedService"

        val actionPayload = formatPath(argument, libPath)

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