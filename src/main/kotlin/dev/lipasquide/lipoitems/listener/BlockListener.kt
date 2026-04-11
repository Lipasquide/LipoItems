package dev.lipasquide.lipoitems.listener

import dev.lipasquide.lipoitems.LipoItems
import dev.lipasquide.lipoitems.model.CustomType
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.NotePlayEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType

class BlockListener(private val plugin: LipoItems) : Listener {
    private val CUSTOM_ID_KEY = NamespacedKey("lipoitems", "id")

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        val meta = item.itemMeta ?: return
        val id = meta.persistentDataContainer.get(CUSTOM_ID_KEY, PersistentDataType.STRING) ?: return
        val config = plugin.itemManager.getItem(id) ?: return

        if (config.type == CustomType.BLOCK && config.blockData != null) {
            val block = event.block
            val blockData = plugin.server.createBlockData(config.blockData)
            block.blockData = blockData
        }
    }

    @EventHandler
    fun onNotePlay(event: NotePlayEvent) {
        // Cancel note play for custom blocks (Note Blocks)
        event.isCancelled = true
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            val block = event.clickedBlock ?: return
            if (block.type == Material.NOTE_BLOCK) {
                // Prevent changing note
                if (!event.player.isSneaking) {
                    // We might want to allow opening GUIs if it's a special block
                }
            }
        }
    }
}
