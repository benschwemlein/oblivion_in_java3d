plugins {
    kotlin("jvm") version "2.0.0"
    application
}

application {
    mainClass.set("MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    // Reuse the Java3D jars bundled with the java/ version of this project
    implementation(fileTree("../java/java3d") { include("**/*.jar") })
}

tasks.jar {
    manifest { attributes["Main-Class"] = "MainKt" }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
