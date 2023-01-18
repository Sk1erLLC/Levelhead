package club.sk1er.mods.levelhead.render

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.client.network.NetworkPlayerInfo

object TabRender {

    fun drawPingHook(offset: Int, x: Int, y: Int, playerInfo: NetworkPlayerInfo) {
        if (!Levelhead.displayManager.config.enabled) return
        Levelhead.displayManager.tab.run {
            if (!this.config.enabled || !Levelhead.LevelheadPurchaseStates.tab ||
                (playerInfo.gameProfile.id == UPlayer.getUUID() && !this.config.showSelf)) return

            val playerUUID = if (playerInfo.gameProfile.id.version() == 2) {
                val playerName = playerInfo.displayName?.unformattedText?.substringAfter("] ")?.substringBefore(' ')?.trim() ?: return
                if (playerName.isBlank()) return
                UMinecraft.getPlayer()?.sendQueue?.getPlayerInfo(playerName)?.gameProfile?.id?.takeIf { it.version() == 4 } ?: return
            } else {
                playerInfo.gameProfile.id
            }

            this.cache[playerUUID]?.footer?.value?.let { str ->
                var x1 = offset + x - 12 - UMinecraft.getFontRenderer().getStringWidth(str)
                UMinecraft.getWorld()?.scoreboard?.run {
                    if (getObjectiveInDisplaySlot(0) != null) {
                        x1 -= UMinecraft.getFontRenderer().getStringWidth(
                            " ${getValueFromObjective(playerInfo.gameProfile.name, getObjectiveInDisplaySlot(0)).scorePoints}"
                        )
                    }
                }

                when {
                    config.headerChroma -> UMinecraft.getFontRenderer().drawString(str,
                        x1.toFloat() - 1, y.toFloat(), Levelhead.ChromaColor, true)
                    else -> UMinecraft.getFontRenderer().drawString(str,
                        x1.toFloat() - 1, y.toFloat(), this.config.headerColor.rgb, true)
                }
            } ?: Levelhead.fetch(listOf(Levelhead.LevelheadRequest(playerUUID.trimmed, this, false)))
        }
    }

    fun getLevelheadWidth(playerInfo: NetworkPlayerInfo)  = when {
        !Levelhead.displayManager.config.enabled -> 0
        (Levelhead.displayManager.tab.config.enabled && Levelhead.LevelheadPurchaseStates.tab && (playerInfo.gameProfile.id != UPlayer.getUUID() || Levelhead.displayManager.tab.config.showSelf)) -> {
                (Levelhead.displayManager.tab.cache[playerInfo.gameProfile.id]?.footer?.value?.let {
                    UMinecraft.getFontRenderer().getStringWidth(
                        it
                    )
                } ?: 0) + 3
            }
        else -> 0
    }
}