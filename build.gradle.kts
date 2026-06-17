plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.serialization") version "2.2.10"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "org.neosahadeo"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:5.4.0")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("com.beust:klaxon:5.5")
//    testImplementation(kotlin("test")
}

//tasks.test {
//    useJUnitPlatform()
//}

kotlin {
    jvmToolchain(21)
}

tasks {
    named<Jar>("jar") {
        manifest {
            attributes["Main-Class"] = "org.neosahadeo.MainKt"
        }
    }
}