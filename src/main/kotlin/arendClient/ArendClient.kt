package org.example.arendClient

interface ArendClient {
    suspend fun callArendAction(libPath : String, argument : Any?, actionId : String) : String
}