package club.sk1er.mods.levelhead.render

import club.sk1er.mods.levelhead.Levelhead
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.client.network.NetworkPlayerInfo

object TabRender {

    fun drawPingHook(offset: Int, x: Int, y: Int, playerInfo: NetworkPlayerInfo) {
        if (!Levelhead.displayManager.config.enabled) return
        Levelhead.displayManager.tab.run {
            if (!this.config.enabled || !Levelhead.LevelheadPurchaseStates.tab ||
                (playerInfo.gameProfile.id == UPlayer.getUUID() && !this.config.showSelf)) return

            this.cache[playerInfo.gameProfile.id]?.footer?.value?.let { str ->
                var x1 = offset + x - 12 - UMinecraft.getFontRenderer().getStringWidth(str)
                UMinecraft.getWorld()?.scoreboard?.run {
                    if (getObjectiveInDisplaySlot(0) != null) {
                        x1 -= UMinecraft.getFontRenderer().getStringWidth(
                            " ${getValueFromObjective(playerInfo.gameProfile.name, getObjectiveInDisplaySlot(0)).scorePoints}"
                        )
                    }
                }

                when {
                    config.headerChroma -> UMinecraft.getFontRenderer().drawString(str, x1, y, Levelhead.ChromaColor)
                    else -> UMinecraft.getFontRenderer().drawString(str, x1, y , this.config.headerColor.rgb)
                }
            } ?: Levelhead.fetch(playerInfo.gameProfile.id, this, false)
        }
    }

    fun getLevelheadWidth(playerInfo: NetworkPlayerInfo)  = when {
        !Levelhead.displayManager.config.enabled -> 0
        (Levelhead.displayManager.tab.config.enabled && Levelhead.LevelheadPurchaseStates.tab) -> {
                UMinecraft.getFontRenderer().getStringWidth(Levelhead.displayManager.tab.cache[playerInfo.gameProfile.id]?.footer?.value) + 2
            }
        else -> 0
    }
}