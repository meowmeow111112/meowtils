import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("org.polyfrost.multi-version")
    id("org.polyfrost.defaults.repo")
    id("org.polyfrost.defaults.java")
    id("org.polyfrost.defaults.loom")
    id("com.github.johnrengelman.shadow")
    java
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project
val mod_archives_name: String by project
val mod_group: String by project

base {
    archivesName.set(mod_archives_name)
}

loom {
    runConfigs.all {
        runDir = "../../run"
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("../../src/main/java"))
        resources.setSrcDirs(listOf("../../src/main/resources"))
    }
}

repositories {
    mavenLocal()
    maven("https://repo.polyfrost.org/releases")
}

val shade by configurations.creating
configurations.implementation.get().extendsFrom(shade)

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")

    add("modCompileOnly", "cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    add("modRuntimeOnly", "me.djtheredstoner:DevAuth-forge-legacy:1.1.2")
    add("compileOnly", "org.spongepowered:mixin:0.7.11-SNAPSHOT")

    if (platform.isLegacyForge) {
        add(shade.name, "cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
    }
}

tasks.processResources {
    inputs.property("version", mod_version)
    inputs.property("mcversion", "1.8.9")

    filesMatching("mcmod.info") {
        expand(
            mapOf(
                "version" to mod_version,
                "mcversion" to "1.8.9"
            )
        )
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("dev")
    configurations = listOf(shade)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.jar {
    if (platform.isLegacyForge) {
        manifest.attributes += mapOf(
            "ModSide" to "CLIENT",
            "ForceLoadAsMod" to true,
            "TweakOrder" to "0",
            "TweakClass" to "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
        )
    }
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")
    // keep the plain jar enabled so we produce a normal (unshaded) artifact as well
    enabled = true
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    archiveClassifier.set("")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Ensure the Minecraft run task uses Java 8 (LaunchWrapper expects a URLClassLoader)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

// Produce a normal sources JAR alongside the shadow/dev and remapped jars
java.withSourcesJar()

// The run task launcher override has been removed.
