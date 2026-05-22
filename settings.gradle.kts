pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
    }
    plugins {
        val pgtVersion = "0.6.5"
        id("org.polyfrost.multi-version.root") version pgtVersion
        id("org.polyfrost.multi-version") version pgtVersion
        id("org.polyfrost.defaults.repo") version pgtVersion
        id("org.polyfrost.defaults.java") version pgtVersion
        id("org.polyfrost.defaults.loom") version pgtVersion
        id("com.github.johnrengelman.shadow") version "8.1.1"
    }
}

val mod_name: String by settings

rootProject.name = mod_name
rootProject.buildFileName = "root.gradle.kts"

listOf(
    "1.8.9-forge"
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}
