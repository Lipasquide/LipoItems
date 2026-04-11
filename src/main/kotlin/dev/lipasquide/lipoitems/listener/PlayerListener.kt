package dev.lipasquide.lipoitems.listener

import dev.lipasquide.lipoitems.LipoItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerListener(private val plugin: LipoItems) : Listener {

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val externalHost = plugin.config.getString("server.external-host", "localhost")
        val port = plugin.config.getInt("server.port", 8080)
        val url = "http://$externalHost:$port/pack.zip"
        val hash = plugin.packGenerator.getPackHash()

        if (hash.isNotEmpty()) {
            event.player.setResourcePack(url, hash, true)
        }
    }
}
