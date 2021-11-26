package club.sk1er.mods.levelhead.commands

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.displayManager
import club.sk1er.mods.levelhead.Levelhead.types
import club.sk1er.mods.levelhead.gui.LevelheadMainGUI
import gg.essential.api.EssentialAPI
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import gg.essential.api.commands.SubCommand
import gg.essential.universal.ChatColor
import kotlinx.coroutines.launch

class LevelheadCommand : Command("levelhead") {

    @DefaultHandler
    fun handle() {
        EssentialAPI.getGuiUtil().openScreen(LevelheadMainGUI())
    }

    @SubCommand(value = "limit")
    fun handleLimit() {
        EssentialAPI.getMinecraftUtil()
            .sendMessage("[levelhead]", ChatColor.RED.toString() + "Callback_types: " + types)
        EssentialAPI.getMinecraftUtil().sendMessage(
            "[levelhead]",
            ChatColor.RED.toString() + "Hypixel: " + EssentialAPI.getMinecraftUtil().isHypixel()
        )
    }

    @SubCommand(value = "reauth")
    fun handleReauth() {
        Levelhead.scope.launch {
            launch {
                Levelhead.refreshRawPurchases()
            }
            launch {
                Levelhead.refreshPaidData()
            }
            launch {
                Levelhead.refreshPurchaseStates()
            }
        }
    }

    @SubCommand(value = "dumpcache")
    fun handleDumpCache() {
        displayManager.clearCache()
        EssentialAPI.getMinecraftUtil().sendMessage("[Levelhead]", ChatColor.RED.toString() + "Cleared Cache")
    }
}