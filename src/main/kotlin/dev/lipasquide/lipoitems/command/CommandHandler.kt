package dev.lipasquide.lipoitems.command

import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.arguments.standard.StringArgument
import cloud.commandframework.bukkit.arguments.PlayerArgument
import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.minecraft.extras.AudienceProvider
import cloud.commandframework.minecraft.extras.MinecraftHelp
import cloud.commandframework.paper.PaperCommandManager
import dev.lipasquide.lipoitems.LipoItems
import dev.lipasquide.lipoitems.util.ItemBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class CommandHandler(private val plugin: LipoItems) {
    private val manager: PaperCommandManager<CommandSender> = PaperCommandManager.createNative(
        plugin,
        CommandExecutionCoordinator.simpleCoordinator()
    )

    init {
        val help = MinecraftHelp(
            "/lipoitems help",
            AudienceProvider.nativeAudience(),
            manager
        )

        manager.command(
            manager.commandBuilder("lipoitems")
                .literal("help")
                .argument(StringArgument.optional("query"))
                .handler { context ->
                    help.queryCommands(context.getOrDefault("query", ""), context.sender)
                }
        )

        registerGiveCommand()
        registerReloadCommand()
    }

    private fun registerGiveCommand() {
        manager.command(
            manager.commandBuilder("lipoitems")
                .literal("give")
                .permission("lipoitems.admin")
                .argument(PlayerArgument.of("player"))
                .argument(StringArgument.of("item"))
                .argument(IntegerArgument.optional("amount", 1))
                .handler { context ->
                    val target = context.get<Player>("player")
                    val itemId = context.get<String>("item")
                    val amount = context.get<Int>("amount")

                    val config = plugin.itemManager.getItem(itemId)
                    if (config == null) {
                        context.sender.sendMessage(Component.text("Item not found: $itemId", NamedTextColor.RED))
                        return@handler
                    }

                    val stack = ItemBuilder.build(config)
                    stack.amount = amount
                    target.inventory.addItem(stack)
                    context.sender.sendMessage(Component.text("Gave $amount x $itemId to ${target.name}", NamedTextColor.GREEN))
                }
        )
    }

    private fun registerReloadCommand() {
        manager.command(
            manager.commandBuilder("lipoitems")
                .literal("reload")
                .permission("lipoitems.admin")
                .handler { context ->
                    plugin.reloadConfig()
                    plugin.itemManager.loadItems()
                    plugin.packGenerator.generate()
                    context.sender.sendMessage(Component.text("LipoItems reloaded!", NamedTextColor.GREEN))
                }
        )
    }
}
