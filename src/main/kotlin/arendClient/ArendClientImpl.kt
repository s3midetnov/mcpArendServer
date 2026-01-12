package org.example.arendClient

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException

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

                // 1. Send Info
                System.err.println("Sending: $definition")
                output.println(definition)
                output.flush()
                socket.shutdownOutput()

                // 2. Read Answer (Will block until data arrives or timeout hits)
                val answer = input.readText()


                val home = System.getProperty("user.home")
                val file = File(home, "Desktop/debug.txt")
                file.writeText("answer is $answer")

                System.err.println("Server answered: $answer")
                return answer
            }
        } catch (e: SocketTimeoutException) {
            System.err.println("Error: The server took too long to respond!")
            println("bee")
        } catch (e: Exception) {
            System.err.println("Connection error: ${e.message}")
            println("eeeeb")
        }
        return "Typechecking error"
    }
}