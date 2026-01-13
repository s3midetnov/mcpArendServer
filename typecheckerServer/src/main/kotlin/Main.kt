import simpleTypechecker.SimpleTypechecker
import java.io.*
import java.net.ServerSocket
import java.util.Base64
import kotlin.concurrent.thread
import kotlin.time.measureTime


// The typechecking server
fun main() {
//    debug:
//    val simpleTypechecker = SimpleTypechecker("/Users/artem.semidetnov/Documents/DatasetGenerator/Arend/arend-lib")
//    println(simpleTypechecker.typecheckToError())
//    return

    val soTimeout = 1000
    val timeout = 30 * 60 * 1000 // 5 minutes


    val port = 9999
    val server = ServerSocket(port)

    val sampleLibrary : String = "/Users/artem.semidetnov/Documents/DatasetGenerator/Arend/arend-lib"
    val simpleTypechecker = SimpleTypechecker(sampleLibrary)
    simpleTypechecker.typecheckToError()
    server.soTimeout = soTimeout // 1 second timeout for accept()
    System.err.println("Server is listening on port $port")

    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeout) {
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
                if (encodedRequest != null) {
                    val request = String(Base64.getDecoder().decode(encodedRequest), Charsets.UTF_8)

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
//                val request = input.readText()
//                simpleTypechecker.writeUserCode(request)
//                System.err.println("Received: $request \n --------------------")
//                val durationAnswer = measureTime {
//                    val response = "Processed: ${simpleTypechecker.typecheckToError()}"
//                    output.println(response)
//                    output.flush()
//                    System.err.println("Sent: $response")
//                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                client.close()
            }
        }
    }
    System.err.println("Server timed out after ${timeout / 1000} seconds. Shutting down.")
}