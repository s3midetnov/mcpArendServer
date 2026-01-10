package org.example.simpleTypechecker

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
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Objects
import java.util.function.Supplier
import java.util.logging.Logger
import kotlin.system.exitProcess

//TODO:
// 1. make so that a user can run this themselves with their own version of a library beforehand
// 2. make so that all dependencies are added automatically
// 3. add timeout parameter
// val checker = server.getCheckerFor(listOf(module))


class SimpleTypechecker (val pathToLibrary: String) {
    val listErrorReporter = ListErrorReporter()
    val libraryManager = LibraryManager(listErrorReporter)

    lateinit var arendServer : ArendServer
    lateinit var library : SourceLibrary
    lateinit var logger: Logger
    val libDir: Path = Paths.get(pathToLibrary)

    var requestedLibraries : MutableList<SourceLibrary> = ArrayList()
    var isInitialized : Boolean = false


    fun createArendServer() {
        library = FileSourceLibrary.fromConfigFile(libDir.resolve(FileUtils.LIBRARY_CONFIG_FILE), true, listErrorReporter)
        requestedLibraries.addLast(library)

        arendServer = ArendServerImpl(CliServerRequester(libraryManager), false, false, true)
        println("prelude module location ${Prelude.MODULE_LOCATION}")

        arendServer.addErrorReporter(listErrorReporter)
        arendServer.addReadOnlyModule(
            Prelude.MODULE_LOCATION,
            Supplier { Objects.requireNonNull<ConcreteGroup?>(PreludeResourceSource().loadGroup(DummyErrorReporter.INSTANCE)) })
//        arendServer.updateLibrary(library, listErrorReporter)
        isInitialized = true
    }

    fun createLibrary() : Nothing  = TODO()

    fun writeUserCode() : Nothing = TODO()

    fun typecheckToError() : String {
        if (!isInitialized) createArendServer()
        assert (isInitialized)

        for (library in requestedLibraries) {
            libraryManager.updateLibrary(library, arendServer)
            for (modulePath in library.findModules(false)) {
                val module = ModuleLocation(library.libraryName, ModuleLocation.LocationKind.SOURCE, modulePath)
                val checker : ArendChecker = arendServer.getCheckerFor(listOf(module))

                library.getSource(modulePath, false)?.load(arendServer, ListErrorReporter())
                val group: ConcreteGroup = arendServer.getRawGroup(module) ?: exitProcess(1)

                group.traverseGroup { x ->
                    x.definition?.let {
                        checker.typecheck(
                            FullName(module, it.data.refLongName),
                            ArendCheckerFactory.DEFAULT,
                            null,
                            listErrorReporter,
                            UnstoppableCancellationIndicator.INSTANCE,
                            ProgressReporter.empty()
                        )
                    }
                }
            }
        }

        val relevantErrorsSublist = mutableListOf<String>()
        for (error in listErrorReporter.errorList){
            val errorDoc = error.getDoc(PrettyPrinterConfig.DEFAULT)
            relevantErrorsSublist.add(errorDoc.toString())
        }
        return relevantErrorsSublist.fold("") { acc, error -> acc  +  error + "\n" }.substring(0, 500)
    }
}