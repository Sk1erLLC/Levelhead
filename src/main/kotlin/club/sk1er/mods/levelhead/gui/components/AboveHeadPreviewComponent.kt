package club.sk1er.mods.levelhead.gui.components

import com.mojang.authlib.GameProfile
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.buildEmulatedPlayer
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.wrappers.UPlayer

class AboveHeadPreviewComponent: LevelheadPreviewComponent() {

    private val player = EssentialAPI.getEssentialComponentFactory().buildEmulatedPlayer {
        profile = GameProfile(UPlayer.getUUID(), UPlayer.getPlayer()!!.displayName.formattedText)

        renderNameTag = false
        showCape = false
        draggable = false
    }.constrain {
        x = CenterConstraint()
        y = (-45).pixels(true,  true)
        width = 35.percentOfWindow
        height = 25.percentOfWindow
    } childOf this effect ScissorEffect(this)

    init {
        constrain {
            width = ChildBasedSizeConstraint()
            height = RelativeConstraint()
        }
    }
}