package dev.lipasquide.lipoitems.config

import dev.lipasquide.lipoitems.LipoItems
import dev.lipasquide.lipoitems.model.CustomItemConfig
import dev.lipasquide.lipoitems.model.CustomType
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ItemManager(private val plugin: LipoItems) {
    private val items = mutableMapOf<String, CustomItemConfig>()
    private val itemsFolder = File(plugin.dataFolder, "items")

    init {
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs()
            // Create an example item
            val exampleFile = File(itemsFolder, "example.yml")
            exampleFile.writeText("""
                ruby_sword:
                  material: DIAMOND_SWORD
                  display-name: "<red>Ruby Sword"
                  lore:
                    - "<gray>A powerful ruby sword"
                  item-model: "lipoitems:item/ruby_sword"

                ruby_chestplate:
                  material: LEATHER_CHESTPLATE
                  display-name: "<red>Ruby Chestplate"
                  type: ARMOR
                  item-model: "lipoitems:item/ruby_chestplate"

                custom_stone:
                  material: NOTE_BLOCK
                  display-name: "Custom Stone"
                  type: BLOCK
                  block-data: "instrument=bit,note=0"
                  item-model: "lipoitems:block/custom_stone"
            """.trimIndent())
        }
    }

    fun loadItems() {
        items.clear()
        itemsFolder.listFiles { _, name -> name.endsWith(".yml") }?.forEach { file ->
            val config = YamlConfiguration.loadConfiguration(file)
            config.getKeys(false).forEach { key ->
                val section = config.getConfigurationSection(key) ?: return@forEach
                val id = key
                val materialStr = section.getString("material", "STONE")
                val material = Material.matchMaterial(materialStr!!) ?: Material.STONE
                val displayName = section.getString("display-name")
                val lore = section.getStringList("lore")
                val customModelData = if (section.contains("custom-model-data")) section.getInt("custom-model-data") else null
                val itemModel = section.getString("item-model")
                val type = CustomType.valueOf(section.getString("type", "ITEM")!!.uppercase())
                val armorType = section.getString("armor-type")
                val blockData = section.getString("block-data")
                val settings = section.getConfigurationSection("settings")?.getValues(false) ?: emptyMap()
                val behavior = section.getConfigurationSection("behavior")?.getValues(false) ?: emptyMap()

                items[id] = CustomItemConfig(
                    id, material, displayName, lore, customModelData, itemModel, type, armorType, blockData, settings, behavior
                )
            }
        }
        plugin.logger.info("Loaded ${items.size} custom items.")
    }

    fun getItem(id: String): CustomItemConfig? = items[id]
    fun getAllItems(): Collection<CustomItemConfig> = items.values
}
