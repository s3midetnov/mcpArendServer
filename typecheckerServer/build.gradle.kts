plugins {
    kotlin("jvm")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.arend:base")
    implementation("org.arend:cli")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}