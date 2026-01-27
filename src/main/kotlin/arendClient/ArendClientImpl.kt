package org.example.arendClient


import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ArendClientImpl : ArendClient {
    override suspend fun typecheck_definition(projectPath : String, modules : String): String {
        createFolderForJunieIfNotExists(projectPath)
        val fileWithAnswers = File(projectPath + "/.junieCommunication/errorFile.txt")
        fileWithAnswers.writeText("")
        val projectName = projectPath.split("/").last()
        println("projectName= $projectName")
        triggerAction(modules + "%%${projectName}")

        Thread.sleep(1000)

        val listWithErrors = fileWithAnswers.readLines()
        return listWithErrors.joinToString("\n")
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

    fun createFolderForJunieIfNotExists(projectPath: String) {
        val communicationFolder = File(projectPath, ".junieCommunication")
        val errorFile = File(communicationFolder, "errorFile.txt")
        if (!communicationFolder.exists()) {
            val created = communicationFolder.mkdirs()
            if (created) {
                println("Folder .junieCommunication created.")
            }
        } else {
            println("Folder .junieCommunication already exists.")
        }

        // Create the file if it doesn't exist
        if (!errorFile.exists()) {
            val created = errorFile.createNewFile()
            if (created) {
                println("File errorFile.txt created.")
            }
        } else {
            println("File errorFile.txt already exists.")
        }
    }
}