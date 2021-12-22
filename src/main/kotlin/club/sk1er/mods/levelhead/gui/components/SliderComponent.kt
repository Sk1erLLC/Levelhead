package club.sk1er.mods.levelhead.gui.components

import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.font.DefaultFonts
import gg.essential.elementa.state.toConstraint
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.AbstractSliderComponent
import gg.essential.vigilance.gui.settings.Slider
import kotlin.math.roundToInt

class SliderComponent(initialValue: Int, min: Int, max: Int) : AbstractSliderComponent() {
    init {
        UIText(min.toString()).constrain {
            y = 0.pixels
            color = VigilancePalette.getMidText().constraint
            fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
        } childOf this
    }

    override val slider by Slider((initialValue.toFloat() - min) / (max - min)).constrain {
        x = SiblingConstraint()
        width = 60.pixels()
        height = 12.pixels()
    } childOf this

    init {
        UIText(max.toString()).constrain {
            x = SiblingConstraint()
            y = 0.pixels
            color = VigilancePalette.getMidText().constraint
            fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
        } childOf this
    }

    private val currentValueText by UIText(initialValue.toString()).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
        color = VigilancePalette.getMidText().constraint
        fontProvider = DefaultFonts.VANILLA_FONT_RENDERER
    } childOf slider.grabBox

    init {
        slider.onValueChange { newPercentage ->
            val newValue = (min + (newPercentage * (max - min))).roundToInt()
            changeValue(newValue)
            currentValueText.setText(newValue.toString())
        }

        sliderInit()
    }
}