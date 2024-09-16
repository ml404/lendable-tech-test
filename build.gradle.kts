plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.layton"
version = "1.0-SNAPSHOT"


val kotlinVersion = "2.0.0"
val kotlinxCoroutinesVersion = "1.9.0" // Use a compatible stable version
val mockk = "1.13.11"


repositories {
    mavenCentral()
}



dependencies {
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:$mockk")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}