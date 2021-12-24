package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.isNPC
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import net.minecraft.entity.player.EntityPlayer
import java.util.*

class ChatDisplay(config: DisplayConfig): LevelheadDisplay(DisplayPosition.CHAT, config) {
    override fun joinWorld() {
        UMinecraft.getMinecraft().netHandler!!.playerInfoMap
            .filter { it.gameProfile.id.version() == 4 && !cache.containsKey(it.gameProfile.id) }
            .map { Levelhead.LevelheadRequest(it.gameProfile.id.trimmed, this, false) }
            .run { Levelhead.fetch(this) }
    }

    override fun playerJoin(player: EntityPlayer) {
        if (player.isNPC) return
        if (!cache.containsKey(player.uniqueID))
            Levelhead.fetch(listOf(Levelhead.LevelheadRequest(player.uniqueID.trimmed, this, false)))
    }
}