import simpleTypechecker.MinimalTypechecker
import simpleTypechecker.SimpleTypechecker
import java.io.*
import java.net.ServerSocket
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.time.measureTime
import kotlin.system.exitProcess


// The typechecking server
// to run use  /Users/artem.semidetnov/Dev/mcpArendServer/typecheckerServer/build/install/typecheckerServer/bin/typecheckerServer -t 7200
fun main(args: Array<String>) {
    var sampleLibrary = "/Users/artem.semidetnov/Documents/DatasetGenerator/Arend/arend-lib"
    val minimalTypechecker = MinimalTypechecker(sampleLibrary)
    minimalTypechecker.typecheckToError()
    return

    val acceptTimeout = 1000 // Time to wait for a client connection (soTimeout)

    var serverLifetimeSeconds = 30 * 60 //30 minutes
    var port = 9999
//    var sampleLibrary = "/Users/artem.semidetnov/Documents/DatasetGenerator/Arend/arend-lib"

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-p", "--port" -> {
                if (i + 1 < args.size) {
                    port = args[i + 1].toIntOrNull() ?: run {
                        System.err.println("Error: Invalid port number.")
                        exitProcess(1)
                    }
                    i++
                } else {
                    System.err.println("Error: Missing value for port.")
                    exitProcess(1)
                }
            }
            "-t", "--timeout" -> {
                if (i + 1 < args.size) {
                    serverLifetimeSeconds = args[i + 1].toIntOrNull() ?: run {
                        System.err.println("Error: Invalid timeout value.")
                        exitProcess(1)
                    }
                    i++
                } else {
                    System.err.println("Error: Missing value for timeout.")
                    exitProcess(1)
                }
            }
            "-l", "--lib" -> {
                if (i + 1 < args.size) {
                    sampleLibrary = args[i + 1]
                    i++
                } else {
                    System.err.println("Error: Missing value for library path.")
                    exitProcess(1)
                }
            }
            "-h", "--help" -> printUsage()
            else -> {
                System.err.println("Unknown argument: ${args[i]}")
                printUsage()
                exitProcess(1)
            }
        }
        i++
    }
    val simpleTypechecker = SimpleTypechecker(sampleLibrary)
    val timeoutMs = serverLifetimeSeconds * 1000L

    val server = ServerSocket(port)
    server.soTimeout = acceptTimeout

    System.err.println("------------------------------------------------")
    System.err.println("Server is listening on port $port")
    System.err.println("Server will shut down in ${serverLifetimeSeconds / 60} minutes")
    System.err.println("------------------------------------------------")

    val durationTypecheck = measureTime {
        simpleTypechecker.typecheckToError()
    }

    System.err.println("The arend-library is successfully typechecked in $durationTypecheck.")

    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeoutMs) {
        val client = try {
            server.accept()
        } catch (e: java.net.SocketTimeoutException) {
            continue
        }
        thread {
            try {
                val input = BufferedReader(InputStreamReader(client.getInputStream()))
                val output = PrintWriter(client.getOutputStream(), true)

                val encodedRequest = input.readLine()
                encodedRequest?.let{
                    val request = String(Base64.getDecoder().decode(encodedRequest), Charsets.UTF_8)
                    System.err.println("Received request:\n$request")


                    if (request == "%%%%library_location") {
                        System.err.println("Sending library location")
                        System.err.println(simpleTypechecker.pathToLibrary.toString())
                        System.err.println("--------------------------------------------------")

                        val encodedPath = Base64.getEncoder().encodeToString(simpleTypechecker.pathToLibrary.toByteArray(Charsets.UTF_8))
                        output.println(encodedPath)
                        return@thread
                    }

                    System.err.println("Received Code:\n$request")

                    simpleTypechecker.writeUserCode(request)

                    val durationAnswer = measureTime {
                        val fullResponse = simpleTypechecker.typecheckToError()

                        // 2. Encode answer before sending
                        val encodedResponse = Base64.getEncoder().encodeToString(fullResponse.toByteArray(Charsets.UTF_8))
                        output.println(encodedResponse)
                    }
                    System.err.println("Typechecking took $durationAnswer")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                client.close()
            }
        }
    }
    System.err.println("Server timed out after ${serverLifetimeSeconds / 60} minutes. Shutting down.")
}
fun printUsage() {
    System.err.println("""
        Usage: java -jar YourServer.jar [options]
        
        Options:
          -p, --port <int>      Port number (default: 9999)
          -t, --timeout <int>   Server lifetime in seconds (default: 1800 / 30 mins)
          -l, --lib <path>      Path to the Arend library (default: hardcoded path)
          -h, --help            Show this help message
    """.trimIndent())
}