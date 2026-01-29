package org.example.arendClient

import kotlinx.coroutines.delay
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.collections.fold

class ArendClientImpl : ArendClient {
    private val doneMarker = "TYPECHECK_DONE"

    override suspend fun typecheck_definition(projectPath : String, modules : List<String>): String {
        val modulesFolded = modules.fold("", { acc, s -> "$acc$s%%" })
        createFolderForJunieIfNotExists(projectPath)
        val fileWithAnswers = File(projectPath + "/.junieCommunication/errorFile.txt")
        fileWithAnswers.writeText("")
        val projectName = projectPath.split("/").last()
        val success = try {
            triggerAction(modulesFolded + projectName)
        } catch (_: Exception) {
            false
        }
        if (!success) {
            return "Failed to trigger typechecking. Make sure IntelliJ is running with the Arend plugin."
        }

        val allowedFiles = modules.map { it.split(".").last() }
        return waitForCompletion(fileWithAnswers, allowedFiles)
    }

    fun triggerAction(actionId: String): Boolean {
        val START_PORT = 63342
        val END_PORT = 63352
        val ENDPOINT = "api/detachedTypechecker" // Must match your plugin's handler path

        // Use the modern Java 11+ HttpClient
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(500))
            .build()

        for (port in START_PORT..END_PORT) {
            val encodedAction = URLEncoder.encode(actionId, StandardCharsets.UTF_8.toString())
            val url = "http://localhost:$port/$ENDPOINT?action=$encodedAction"

            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build()

                // Send request
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())

                when (response.statusCode()) {
                    200 -> {
                        return true
                    }
                    404, 401, 403 -> {
                        continue
                    }
                    else -> {
                        return false
                    }
                }

            } catch (_: java.net.ConnectException) {
                continue
            } catch (_: Exception) {
            }
        }
        return false
    }

    fun filterLinesOnlyFile(lines: List<String>, allowedFiles: List<String>, completionMarker: String? = null): List<String> =
        lines
            .filterNot { completionMarker != null && it.trim() == completionMarker }
            .runningFold(false to "") { (keep, _), line ->
                if (line.startsWith("[")) {
                    val parts = line.split(" ", limit = 3)
                    if (parts.size >= 2) {
                        val fileNameWithRest = parts[1]
                        val name = fileNameWithRest.substringBefore(".ard")
                        val hasArd = fileNameWithRest.contains(".ard")
                        (hasArd && name in allowedFiles) to line
                    } else {
                        false to line
                    }
                } else {
                    keep to line
                }
            }
            .drop(1)
            .filter { it.first }
            .map { it.second }

    private suspend fun waitForCompletion(
        fileWithAnswers: File,
        allowedFiles: List<String>,
        timeoutMillis: Long = 30000,
        pollDelayMillis: Long = 500
    ): String {
        val startTime = System.currentTimeMillis()
        var listWithErrors = fileWithAnswers.readLines()
        var ans = filterLinesOnlyFile(listWithErrors, allowedFiles, doneMarker).joinToString("\n")

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            if (listWithErrors.any { it.trim() == doneMarker }) {
                return if (ans.isEmpty()) "Typechecked successfully" else ans
            }

            delay(pollDelayMillis)
            listWithErrors = fileWithAnswers.readLines()
            ans = filterLinesOnlyFile(listWithErrors, allowedFiles, doneMarker).joinToString("\n")
        }

        return if (ans.isEmpty()) "Typechecking timed out before completion" else ans
    }

    fun createFolderForJunieIfNotExists(projectPath: String) {
        val communicationFolder = File(projectPath, ".junieCommunication")
        val errorFile = File(communicationFolder, "errorFile.txt")
        if (!communicationFolder.exists()) {
            communicationFolder.mkdirs()
            if (!errorFile.exists()) {
                errorFile.createNewFile()
            }
        }
    }
}