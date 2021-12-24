package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.isNPC
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import net.minecraft.entity.player.EntityPlayer
import java.util.*

class ChatDisplay(config: DisplayConfig): LevelheadDisplay(DisplayPosition.CHAT, config) {

    override fun toString(): String = "chat"

    override fun playerJoin(player: EntityPlayer) {
        if (player.isNPC) return
        if (!cache.containsKey(player.uniqueID))
            Levelhead.fetch(listOf(Levelhead.LevelheadRequest(player.uniqueID.trimmed, this, false)))
    }
}