package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import gg.essential.universal.ChatColor
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import java.util.*


fun LevelheadDisplay.update() {
    this.cache.remove(UPlayer.getUUID())
    Levelhead.fetch(UPlayer.getUUID(), this, if (this is AboveHeadDisplay) this.bottomValue else false)
}

fun Color.tryToGetChatColor() =
    ChatColor.values().filter { it.isColor() }.find { it.color!! == this }

val UUID.trimmed: String
    get() = this.toString().replace("-", "")

val EntityPlayer.isNPC: Boolean
    get() = this.uniqueID.trimmed[12] == '2'