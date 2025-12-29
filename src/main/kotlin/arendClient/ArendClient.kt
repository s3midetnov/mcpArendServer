package org.example.arendClient

interface ArendClient {
    suspend fun typecheck(file: String): String
}