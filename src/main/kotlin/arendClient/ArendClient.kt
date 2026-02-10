package org.example.arendClient

interface ArendClient {
    suspend fun typecheck_definition(projectPath : String, modules: List<String>): String

    suspend fun proof_search(projectPath: String, query : String) : String
}