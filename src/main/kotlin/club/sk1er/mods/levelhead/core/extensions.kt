package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import gg.essential.universal.wrappers.UPlayer


fun LevelheadDisplay.update() {
    this.cache.remove(UPlayer.getUUID())
    Levelhead.fetch(UPlayer.getUUID(), this, if (this is AboveHeadDisplay) this.bottomValue else false)
}