package dev.lipasquide.lipoitems

import dev.lipasquide.lipoitems.command.CommandHandler
import dev.lipasquide.lipoitems.config.ItemManager
import dev.lipasquide.lipoitems.listener.BlockListener
import dev.lipasquide.lipoitems.listener.PlayerListener
import dev.lipasquide.lipoitems.pack.PackGenerator
import dev.lipasquide.lipoitems.server.PackServer
import org.bukkit.plugin.java.JavaPlugin

class LipoItems : JavaPlugin() {

    companion object {
        lateinit var instance: LipoItems
            private set
    }

    lateinit var itemManager: ItemManager
        private set
    lateinit var packGenerator: PackGenerator
        private set
    lateinit var packServer: PackServer
        private set
    lateinit var commandHandler: CommandHandler
        private set

    override fun onEnable() {
        instance = this

        saveDefaultConfig()

        itemManager = ItemManager(this)
        itemManager.loadItems()

        packGenerator = PackGenerator(this)
        packGenerator.generate()

        packServer = PackServer(this)
        packServer.start()

        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.pluginManager.registerEvents(BlockListener(this), this)

        commandHandler = CommandHandler(this)

        logger.info("LipoItems has been enabled!")
    }

    override fun onDisable() {
        if (::packServer.isInitialized) {
            packServer.stop()
        }
        logger.info("LipoItems has been disabled!")
    }
}
