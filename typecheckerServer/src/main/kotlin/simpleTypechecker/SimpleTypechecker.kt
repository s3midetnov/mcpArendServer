package simpleTypechecker

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
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import java.util.Objects
import java.util.function.Supplier
import java.util.logging.Logger
import kotlin.system.exitProcess

//TODO:
// 1. make so that a new library is created instead of adding files in arend-lib
// 2. make so that all dependencies are added automatically
// 3. give LLM an ability to typecheck a whole file via adding it to the arend-lib


class SimpleTypechecker (val pathToLibrary: String, val isDebugging: Boolean = false) {
    val listErrorReporter = ListErrorReporter()
    val libraryManager = LibraryManager(listErrorReporter)

    lateinit var arendServer : ArendServer
        private set
    lateinit var library : SourceLibrary
        private set
    lateinit var logger: Logger
        private set
    val libDir: Path = Paths.get(pathToLibrary)

    var requestedLibraries : MutableList<SourceLibrary> = ArrayList()
        private set
    var isLibraryTypechecked : Boolean = false
        private set
    var isServerInitialized : Boolean = false
        private set


    fun createArendServer() {
        if (!isDebugging) {
            val file = File("$pathToLibrary/src/userCode.ard")
            file.writeText("")
        }

        library = FileSourceLibrary.fromConfigFile(libDir.resolve(FileUtils.LIBRARY_CONFIG_FILE), true, listErrorReporter)
        requestedLibraries.addLast(library)

        arendServer = ArendServerImpl(CliServerRequester(libraryManager), false, false, true)

        arendServer.addErrorReporter(listErrorReporter)
        arendServer.addReadOnlyModule(
            Prelude.MODULE_LOCATION,
            Supplier { Objects.requireNonNull<ConcreteGroup?>(PreludeResourceSource().loadGroup(DummyErrorReporter.INSTANCE)) })
        isServerInitialized = true
    }

    fun writeUserCode(code : String = ""){
        val file = File("$pathToLibrary/src/userCode.ard")
        file.writeText(code)
    }

    fun typecheckToError() : String {
        listErrorReporter.errorList.clear()
        if (!isServerInitialized) {
            System.err.println("Initializing server...")
            createArendServer()
        }else{
            assert (isServerInitialized)
            System.err.println("Server already initialized")
        }


        val allModules = requestedLibraries.flatMap { library ->
            library.findModules(false).map { it to library }
        }.filter { (modulePath, _) ->
            !(isLibraryTypechecked && !modulePath.toString().contains("userCode"))
        }

        val totalModules = allModules.size
        var currentModuleIndex = 0
        var lastUpdatedLibrary: SourceLibrary? = null

        for ((modulePath, library) in allModules) {
            currentModuleIndex++
            val module = ModuleLocation(library.libraryName, ModuleLocation.LocationKind.SOURCE, modulePath)
            
            // Progress bar
            val percentage = (currentModuleIndex * 100) / totalModules
            val barLength = 20
            val filledLength = (currentModuleIndex * barLength) / totalModules
            val bar = "[" + "=".repeat(filledLength) + " ".repeat(barLength - filledLength) + "]"
            val moduleStr = module.toString()
            val maxModuleLen = 40
            val truncatedModule = if (moduleStr.length > maxModuleLen) "..." + moduleStr.takeLast(maxModuleLen - 3) else moduleStr
            System.err.println("\r$bar $percentage% ($currentModuleIndex/$totalModules) Typechecking $truncatedModule" + " ".repeat(maxModuleLen - truncatedModule.length + 5))

            if (library != lastUpdatedLibrary) {
                libraryManager.updateLibrary(library, arendServer)
                lastUpdatedLibrary = library
            }
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
        if (totalModules > 0) {
            System.err.println()
        }

        isLibraryTypechecked = true

        val relevantErrorsSublist = mutableListOf<String>()
        for (error in listErrorReporter.errorList){
            val errorDoc = error.getDoc(PrettyPrinterConfig.DEFAULT)
            if (errorDoc.toString().contains("userCode"))
                relevantErrorsSublist.add(errorDoc.toString())
        }
        if (relevantErrorsSublist.isEmpty()) return "Typechecked successfully"
        val typecheckingErrors = relevantErrorsSublist.fold("") { acc, error -> acc  +  error + "\n" }
        System.err.println("Typechecking errors: $typecheckingErrors")
        return typecheckingErrors
    }
}