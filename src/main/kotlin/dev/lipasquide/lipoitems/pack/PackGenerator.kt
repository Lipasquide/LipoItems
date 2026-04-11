package dev.lipasquide.lipoitems.pack

import com.google.gson.JsonObject
import dev.lipasquide.lipoitems.LipoItems
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackGenerator(private val plugin: LipoItems) {
    private val packFolder = File(plugin.dataFolder, "pack")
    private val outputFolder = File(plugin.dataFolder, "generated")
    private var packHash: String = ""

    init {
        if (!packFolder.exists()) {
            packFolder.mkdirs()
            File(packFolder, "textures/item").mkdirs()
            File(packFolder, "textures/block").mkdirs()
            File(packFolder, "models/item").mkdirs()
            File(packFolder, "models/block").mkdirs()
        }
        if (!outputFolder.exists()) outputFolder.mkdirs()
    }

    fun generate() {
        val tempDir = File(plugin.dataFolder, "temp_pack")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        // mcmeta
        val mcmeta = File(tempDir, "pack.mcmeta")
        mcmeta.writeText("""
            {
              "pack": {
                "pack_format": 42,
                "description": "LipoItems Generated Pack"
              }
            }
        """.trimIndent())

        // Assets structure in minecraft namespace as requested
        val assetsDir = File(tempDir, "assets/minecraft")
        assetsDir.mkdirs()

        // Copy user provided textures/models
        packFolder.copyRecursively(assetsDir, true)

        // Generate automatic item model definitions if needed
        generateItemModels(assetsDir)

        // Zip it up
        val zipFile = File(outputFolder, "pack.zip")
        zipFile(tempDir, zipFile)

        packHash = calculateHash(zipFile)
        plugin.logger.info("Generated resource pack. Hash: $packHash")

        tempDir.deleteRecursively()
    }

    private fun generateItemModels(assetsDir: File) {
        val itemsDir = File(assetsDir, "items")
        if (!itemsDir.exists()) itemsDir.mkdirs()

        plugin.itemManager.getAllItems().forEach { item ->
            if (item.itemModel != null) {
                val modelFile = File(itemsDir, "${item.id}.json")
                if (!modelFile.exists()) {
                    modelFile.writeText("""
                        {
                          "model": {
                            "type": "minecraft:model",
                            "model": "${item.itemModel}"
                          }
                        }
                    """.trimIndent())
                }
            }
        }
    }

    private fun zipFile(sourceDir: File, outputFile: File) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            sourceDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val entryName = sourceDir.toPath().relativize(file.toPath()).toString().replace("\\", "/")
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
    }

    private fun calculateHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { fis ->
            val buffer = ByteArray(1024)
            var n = fis.read(buffer)
            while (n != -1) {
                digest.update(buffer, 0, n)
                n = fis.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getPackHash() = packHash
}
