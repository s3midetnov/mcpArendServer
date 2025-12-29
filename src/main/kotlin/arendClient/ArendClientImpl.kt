package org.example.arendClient

class ArendClientImpl : ArendClient {
    override suspend fun typecheck(file: String): String {
        return "Typechecked $file + chipi + chipi"
    }
}