package org.example

import org.arend.error.DummyErrorReporter
import org.arend.ext.error.ListErrorReporter
import org.arend.ext.module.FullName
import org.arend.ext.module.ModuleLocation
import org.arend.ext.prettyprinting.PrettyPrinterConfig
import org.arend.frontend.library.CliServerRequester
import org.arend.frontend.library.FileSourceLibrary
import org.arend.frontend.library.LibraryManager
import org.arend.frontend.library.SourceLibrary
import org.arend.frontend.source.PreludeResourceSource
import org.arend.prelude.Prelude
import org.arend.server.ArendChecker
import org.arend.server.ArendServer
import org.arend.server.ProgressReporter
import org.arend.server.impl.ArendServerImpl
import org.arend.term.group.ConcreteGroup
import org.arend.typechecking.computation.UnstoppableCancellationIndicator
import org.arend.typechecking.visitor.ArendCheckerFactory
import org.arend.util.FileUtils
import org.example.arendClient.ArendVisitor
import org.example.simpleTypechecker.SimpleTypechecker
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Objects
import java.util.function.Supplier
import kotlin.system.exitProcess

fun main() {
//    val simpleTypechecker = SimpleTypechecker("/Users/artem.semidetnov/Dev/mcpArendServer/src/testlib")
    val simpleTypechecker = SimpleTypechecker("/Users/artem.semidetnov/IdeaProjects/NilpotentGroups")
    println(simpleTypechecker.typecheckToError())
}