package org.example.arendClient

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Base64

class ArendClientImpl : ArendClient {
    override suspend fun typecheck_definition(definition: String): String {
        val host = "localhost"
        val port = 9999
        try {
            Socket(host, port).use { socket ->
                // --- TIMEOUT LOGIC ---
                // If read() takes longer than 2000ms, it throws SocketTimeoutException
                socket.soTimeout = 10_000

                val output = PrintWriter(socket.getOutputStream(), true)
                val input = BufferedReader(InputStreamReader(socket.getInputStream()))

                // 1. Send definition
                System.err.println("Sending: $definition")
                val encodedDefinition = Base64.getEncoder().encodeToString(definition.toByteArray(Charsets.UTF_8))
                output.println(encodedDefinition)

                // 2. Read Answer (Will block until data arrives or timeout hits)
                val encodedAnswer = input.readLine()
                if (encodedAnswer != null) {
                    val decodedAnswer = String(Base64.getDecoder().decode(encodedAnswer), Charsets.UTF_8)
                    System.err.println("Server answered:\n$decodedAnswer")
                    System.err.println("--------------------------------------------------")
                    return decodedAnswer
                }

//                val home = System.getProperty("user.home")
//                val file = File(home, "Desktop/debug.txt")
//                file.writeText("answer is $answer")
            }
        } catch (e: SocketTimeoutException) {
            System.err.println("Error: The server took too long to respond!")
        } catch (e: Exception) {
            System.err.println("Connection error: ${e.message}")
        }
        return "Typechecking error"
    }

    override suspend fun return_library_location(): String  = "/Users/artem.semidetnov/Documents/DatasetGenerator/Arend/arend-lib"
}