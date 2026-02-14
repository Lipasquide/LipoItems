plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
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
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    // Paper API with NMS access
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Adventure for text components
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")

    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.11.0")

    // FastUtil for collections
    implementation("it.unimi.dsi:fastutil:8.5.14")

    // NBT handling
    implementation("io.github.rapha149.sign:signplugin:1.2.0")

    // Cloud commands (optional, for better command handling)
    implementation("cloud.commandframework:cloud-paper:2.0.0-beta.10")
    implementation("cloud.commandframework:cloud-minecraft-extras:2.0.0-beta.10")
}

tasks {
    compileJava {
        options.release.set(21)
        options.encoding = "UTF-8"
    }

    javadoc {
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

        // Shade dependencies
        relocate("net.kyori", "dev.lipasquide.lipoitems.libs.kyori")
        relocate("com.google.gson", "dev.lipasquide.lipoitems.libs.gson")
        relocate("it.unimi.dsi", "dev.lipasquide.lipoitems.libs.fastutil")

        dependencies {
            include(dependency("net.kyori:.*"))
            include(dependency("com.google.code.gson:.*"))
            include(dependency("it.unimi.dsi:.*"))
        }
    }

    assemble {
        dependsOn(shadowJar)
    }

    // Reobf for NMS
    reobfJar {
        outputJar.set(file("build/libs/${project.name}-${project.version}.jar"))
    }
}

// Mojang mappings for 1.21+
tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}
