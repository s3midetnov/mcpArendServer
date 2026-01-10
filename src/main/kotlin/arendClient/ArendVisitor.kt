package org.example.arendClient
import org.arend.core.definition.CallableDefinition
import org.arend.core.definition.ClassDefinition
import org.arend.core.expr.ClassCallExpression
import org.arend.core.expr.DefCallExpression
import org.arend.core.expr.Expression
import org.arend.core.expr.ReferenceExpression
import org.arend.core.expr.visitor.FreeVariablesCollector
import org.arend.error.DummyErrorReporter
import org.arend.ext.core.expr.CoreExpression
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
import org.arend.server.impl.ArendCheckerImpl
import org.arend.server.impl.ArendServerImpl
import org.arend.term.abs.Abstract
import org.arend.term.concrete.Concrete
import org.arend.term.concrete.Concrete.ResolvableDefinition
import org.arend.term.group.ConcreteGroup
import org.arend.term.prettyprint.ToAbstractVisitor
import org.arend.typechecking.computation.UnstoppableCancellationIndicator
import org.arend.typechecking.visitor.ArendCheckerFactory
import org.arend.typechecking.visitor.CheckTypeVisitor
import org.arend.typechecking.visitor.SearchVisitor
import org.arend.util.FileUtils
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Supplier
import kotlin.system.exitProcess


class ArendVisitor (pathToLibrary: String = "/Users/artem.semidetnov/Dev/Arend/arend-lib") {

    // TODO: add automatic dependency inclusion
    fun writeArendFunction(code : String = """\func testx18 (x : Nat) : Nat => x Nat.+ five""" ) {
        val file = File("/Users/artem.semidetnov/Dev/Arend/arend-lib/src/Algebra/Group/x1817y16.ard")
        file.writeText(code)
    }

    fun typeCheckFile(nameOfFile : String) : String{
        try {
            val libDir: Path = Paths.get("/Users/artem.semidetnov/Dev/Arend/arend-lib")
            val libraryManager = LibraryManager(ListErrorReporter())

            val server: ArendServer = ArendServerImpl(CliServerRequester(libraryManager), false, false, true)
            server.addReadOnlyModule(
                Prelude.MODULE_LOCATION,
                Supplier { Objects.requireNonNull<ConcreteGroup?>(PreludeResourceSource().loadGroup(DummyErrorReporter.INSTANCE)) })// тайпчекинг

            val listErrorReporter = ListErrorReporter()
            server.addErrorReporter(listErrorReporter)

            val requestedLibraries: MutableList<SourceLibrary> = ArrayList()
            val library: SourceLibrary =
                FileSourceLibrary.fromConfigFile(libDir.resolve(FileUtils.LIBRARY_CONFIG_FILE), false, ListErrorReporter())
            requestedLibraries.addLast(library)

            for (library in requestedLibraries) {
                libraryManager.updateLibrary(library, server)
                module@ for (modulePath in library.findModules(false)) {
                    val module = ModuleLocation(library.libraryName, ModuleLocation.LocationKind.SOURCE, modulePath)

                    if (!module.toString().contains("Algebra.Group")) continue@module

                    val checker : ArendChecker = server.getCheckerFor(listOf(module))

                    library.getSource(modulePath, false)?.load(server, ListErrorReporter())
                    val group: ConcreteGroup = server.getRawGroup(module) ?: exitProcess(1)

                    group.traverseGroup { x ->
                        x.definition?.let {
                            checker.typecheck(
                                FullName(module, it.data.refLongName),
                                ArendCheckerFactory.DEFAULT,
                                null,
                                ListErrorReporter(),
                                UnstoppableCancellationIndicator.INSTANCE,
                                ProgressReporter.empty()
                            )
                        }
                    }

                }
            }
//            val relevantErrorsSublist = listErrorReporter.getErrorList()
//                .filter {
//                it.message.contains("x18")
//            }

            val relevantErrorsSublist = mutableListOf<String>()
            for (error in listErrorReporter.errorList){
                val errorDoc = error.getDoc(PrettyPrinterConfig.DEFAULT)
                if (errorDoc.toString().contains("x18")) relevantErrorsSublist.add(errorDoc.toString())
            }
            return relevantErrorsSublist.fold("") { acc, error -> acc + " " +  error + "\n" }
        } catch (e: Exception) {
            e.printStackTrace()
            return e.message ?: "Unknown error"
        }
    }
}