plugins {
    kotlin("jvm") version "2.0.0"
    application
}

application {
    mainClass.set("MainKt")
}

val lwjglVersion = "3.3.3"
val jomlVersion  = "1.10.5"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl").forEach { lib ->
        implementation("org.lwjgl:$lib")
        runtimeOnly("org.lwjgl:$lib::natives-windows")
        runtimeOnly("org.lwjgl:$lib::natives-macos")
        runtimeOnly("org.lwjgl:$lib::natives-macos-arm64")
        runtimeOnly("org.lwjgl:$lib::natives-linux")
    }
    implementation("org.joml:joml:$jomlVersion")
}

tasks.jar {
    manifest { attributes["Main-Class"] = "MainKt" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
