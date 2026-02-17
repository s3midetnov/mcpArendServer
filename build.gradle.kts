plugins {
    kotlin("jvm") version "2.1.10"
    application
    kotlin("plugin.serialization") version "2.1.10"
}

application{
    mainClass.set("org.example.arendMCP.McpServerKt")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
    // Redirect standard output to a pipe so we can see it if it's not JSON-RPC, 
    // but for MCP we usually want it to go to the actual stdout.
    // However, Gradle might be buffering or adding its own output.
    standardOutput = System.out
}

group = "org.example"
version = "1.0-SNAPSHOT"
val ktorVersion = "3.2.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.modelcontextprotocol:kotlin-sdk:0.6.0")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.slf4j:slf4j-nop:2.0.9")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    implementation("org.arend:base")
    implementation("org.arend:cli")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}