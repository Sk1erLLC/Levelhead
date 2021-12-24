package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.isNPC
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import net.minecraft.entity.player.EntityPlayer

class TabDisplay(config: DisplayConfig): LevelheadDisplay(DisplayPosition.TAB, config) {

    override fun toString(): String = "tab"

}