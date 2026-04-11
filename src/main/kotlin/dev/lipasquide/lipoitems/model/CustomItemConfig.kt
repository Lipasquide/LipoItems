package dev.lipasquide.lipoitems.model

import org.bukkit.Material

data class CustomItemConfig(
    val id: String,
    val material: Material,
    val displayName: String?,
    val lore: List<String>?,
    val customModelData: Int?,
    val itemModel: String?,
    val type: CustomType = CustomType.ITEM,
    val armorType: String? = null,
    val blockData: String? = null,
    val settings: Map<String, Any> = emptyMap(),
    val behavior: Map<String, Any> = emptyMap()
)

enum class CustomType {
    ITEM, ARMOR, BLOCK
}
