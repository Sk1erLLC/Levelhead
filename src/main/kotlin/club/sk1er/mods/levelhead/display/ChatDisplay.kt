package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.isNPC
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import net.minecraft.entity.player.EntityPlayer

class ChatDisplay(config: DisplayConfig): LevelheadDisplay(DisplayPosition.CHAT, config) {
    override fun joinWorld() {
        for (networkPlayerInfo in UMinecraft.getMinecraft().netHandler.playerInfoMap) {
            val id = networkPlayerInfo.gameProfile.id
            if (id.version() == 2) continue
            if (!cache.containsKey(id))
                Levelhead.fetch(id, this, false)
        }
    }

    override fun playerJoin(player: EntityPlayer) {
        if (player.isNPC) return
        if (!cache.containsKey(player.uniqueID))
            Levelhead.fetch(player.uniqueID, this, false)
    }
}