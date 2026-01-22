package org.example.arendClient

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.Base64

class ArendClientImpl : ArendClient {
    override suspend fun typecheck_definition(definition: String): String =  File("/Users/artem.semidetnov/Dev/mcpArendServer/src/main/kotlin/errorList.txt").readText()
}