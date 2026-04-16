plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "dev.lipasquide"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://jitpack.io")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // Adventure for text components
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // PacketEvents
    implementation("com.github.retrooper:packetevents-spigot:2.12.0")
}

tasks {
    compileJava {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "version" to project.version,
            "name" to project.name
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        destinationDirectory.set(file("build/libs"))

        relocate("com.github.retrooper.packetevents", "dev.lipasquide.lipoitems.libs.packetevents")
        relocate("io.github.retrooper.packetevents", "dev.lipasquide.lipoitems.libs.packetevents.io")

        dependencies {
            include(dependency("com.github.retrooper:packetevents-spigot:.*"))
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
