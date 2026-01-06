package org.example.arendClient

class ArendClientImpl : ArendClient {

    val arendVisitor = ArendVisitor("../Arend/arend-lib")

    override suspend fun typecheck_definition(code: String): String {
        arendVisitor.writeArendFunction(code)

        val errorTrace = arendVisitor.typeCheckFile("x1817y16.ard")

        return errorTrace
    }
}