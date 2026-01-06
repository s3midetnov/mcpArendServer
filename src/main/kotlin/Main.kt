package org.example

import org.example.arendClient.ArendVisitor

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val arendVisitor = ArendVisitor("../Arend/arend-lib")
    arendVisitor.writeArendFunction()
    println(arendVisitor.typeCheckFile("x1817y16.ard"))
}