package org.example.arendClient

interface ArendClient {
    suspend fun callArendAction(argument: Any?, actionId: String): String
}