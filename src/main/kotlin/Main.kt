package org.example

//import org.example.simpleTypechecker.SimpleTypechecker
import org.example.arendClient.ArendClientImpl
import kotlin.time.measureTime

suspend fun main() {
//    echo '\import Paths \n\\func test (n : Nat) : n = n => idp\n\n\\func test2 (n : Nat) : n = 234 => 15' > ../Documents/DatasetGenerator/Arend/arend-lib/src/userCode.ard
    val sampleInput = "\\func test(n : Nat) : n = 235 => {?}"
    val arendClient = ArendClientImpl()
    println(arendClient.typecheck_definition(sampleInput))
}