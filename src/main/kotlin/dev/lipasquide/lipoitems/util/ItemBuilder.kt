package dev.lipasquide.lipoitems.util

import dev.lipasquide.lipoitems.model.CustomItemConfig
import dev.lipasquide.lipoitems.model.CustomType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType

object ItemBuilder {
    private val mm = MiniMessage.miniMessage()
    private val CUSTOM_ID_KEY = NamespacedKey("lipoitems", "id")

    fun build(config: CustomItemConfig): ItemStack {
        val stack = ItemStack(config.material)
        val meta = stack.itemMeta ?: return stack

        config.displayName?.let {
            meta.displayName(mm.deserialize(it))
        }

        config.lore?.let { lore ->
            meta.lore(lore.map { mm.deserialize(it) })
        }

        config.customModelData?.let {
            meta.setCustomModelData(it)
        }

        // Set the item model key if provided (1.21.4+ API)
        config.itemModel?.let { modelPath ->
             try {
                val method = meta.javaClass.getMethod("setItemModel", NamespacedKey::class.java)
                method.invoke(meta, NamespacedKey.fromString(modelPath))
            } catch (ignored: Exception) {}
        }

        // Handle Armor specific coloring if leather
        if (config.type == CustomType.ARMOR && meta is LeatherArmorMeta) {
            val colorStr = config.settings["color"] as? String
            if (colorStr != null) {
                try {
                    val color = Color.decode(colorStr)
                    meta.setColor(color)
                } catch (ignored: Exception) {}
            }
        }

        meta.persistentDataContainer.set(CUSTOM_ID_KEY, PersistentDataType.STRING, config.id)

        stack.itemMeta = meta
        return stack
    }
}
