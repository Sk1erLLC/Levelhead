package club.sk1er.mods.levelhead.commands

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.displayManager
import club.sk1er.mods.levelhead.Levelhead.types
import club.sk1er.mods.levelhead.gui.LevelheadGUI
import gg.essential.api.EssentialAPI
import gg.essential.api.commands.Command
import gg.essential.api.commands.DefaultHandler
import gg.essential.api.commands.SubCommand
import gg.essential.universal.ChatColor
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

class LevelheadCommand : Command("levelhead") {

    @DefaultHandler
    fun handle() {
        EssentialAPI.getGuiUtil().openScreen(LevelheadGUI())
    }

    @SubCommand(value = "limit")
    fun handleLimit() {
        EssentialAPI.getMinecraftUtil()
            .sendMessage("${ChatColor.AQUA}[Levelhead]", "${ChatColor.RED}Callback_types: " + types)
        EssentialAPI.getMinecraftUtil().sendMessage(
            "${ChatColor.AQUA}[Levelhead]",
            "${ChatColor.RED}Hypixel: " + EssentialAPI.getMinecraftUtil().isHypixel()
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
        }.invokeOnCompletion {
            if (it == null)
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.AQUA}[Levelhead]", "${ChatColor.GREEN} Reauthed!")
            else
                EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.AQUA}[Levelhead]", "${ChatColor.RED} Reauth failed!")
        }
    }

    @SubCommand(value = "dumpcache")
    fun handleDumpCache() {
        Levelhead.scope.coroutineContext.cancelChildren()
        Levelhead.rateLimiter.resetState()
        displayManager.clearCache()
        EssentialAPI.getMinecraftUtil().sendMessage("${ChatColor.AQUA}[Levelhead]", "${ChatColor.GREEN} Cleared Cache")
    }
}