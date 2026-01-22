package org.example.arendClient

interface ArendClient {
    // definition is a string representation of an Arend definition, the return string is the error trace
    suspend fun typecheck_definition(definition: String): String
}