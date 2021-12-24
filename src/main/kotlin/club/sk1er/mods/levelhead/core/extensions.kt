package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.display.LevelheadDisplay
import gg.essential.universal.ChatColor
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import java.util.*


fun LevelheadDisplay.update() {
    this.cache[UPlayer.getUUID()]?.let { tag ->
        tag.header.let { header ->
            header.chroma = this.config.headerChroma
            header.color = this.config.headerColor
            header.value = "${this.config.headerString}: "
        }
        tag.footer.let { footer ->
            footer.chroma = this.config.footerChroma
            footer.color = this.config.footerColor
        }
    }
}

fun Color.tryToGetChatColor() =
    ChatColor.values().filter { it.isColor() }.find { it.color!! == this }

val String.dashUUID: UUID?
    get() {
        if (this.length != 32) return null
        val arr = this.toCharArray().toMutableList()
        arr.add(20, '-')
        arr.add(16, '-')
        arr.add(12, '-')
        arr.add(8, '-')
        return UUID.fromString(arr.joinToString(""))
    }

val UUID.trimmed: String
    get() = this.toString().replace("-", "")

val EntityPlayer.isNPC: Boolean
    get() = this.uniqueID.version() == 2