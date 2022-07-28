package club.sk1er.mods.levelhead.gui.components.settings

import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.font.DefaultFonts
import gg.essential.universal.USound
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.Slider
import gg.essential.vigilance.utils.onLeftClick
import kotlin.math.roundToInt

class SliderComponent(initialValue: Int, min: Int, max: Int) : UIContainer() {
    private var onValueChange: (Int) -> Unit = {}
    private var lastValue: Int? = null
    private var expanded = false
    private var mouseHeld = false

    private val minText by UIText(min.toString()).constrain {
        y = CenterConstraint()
        color = VigilancePalette.getMidText().constraint
        fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
    } childOf this

    private val slider by Slider((initialValue.toFloat() - min) / (max - min)).constrain {
        x = SiblingConstraint()
        width = FillConstraint()
        height = 12.pixels()
    } childOf this

    private val maxText by UIText(max.toString()).constrain {
        x = SiblingConstraint()
        y = CenterConstraint()
        color = VigilancePalette.getMidText().constraint
        fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
    } childOf this

    private val currentValueText by UIText(initialValue.toString()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        color = VigilancePalette.getMidText().constraint
        fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
    } childOf slider.grabBox

    init {
        slider.onValueChange { newPercentage ->
            val newValue = (min + (newPercentage * (max - min))).roundToInt()
            if (newValue != lastValue) {
                this.onValueChange(newValue)
            }
            currentValueText.setText(newValue.toString())
        }

        onLeftClick {
            USound.playButtonPress()
            mouseHeld = true
        }
        onMouseRelease {
            mouseHeld = false
            if (expanded && !slider.isHovered()) {
                slider.animate {
                    setWidthAnimation(Animations.OUT_EXP, .25f, 60.pixels())
                }
                expanded = false
            }
        }
        constrain {
            height = ChildBasedMaxSizeConstraint()
        }
    }

    fun onValueChange(listener: (Int) -> Unit) = apply {
        this.onValueChange = listener
    }
}