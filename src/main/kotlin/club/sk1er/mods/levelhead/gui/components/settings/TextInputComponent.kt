package club.sk1er.mods.levelhead.gui.components.settings

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.input.AbstractTextInput
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.constraints.RelativeConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.utils.onLeftClick

class TextInputComponent(
    private val initial: String,
    placeholder: String
) : UIContainer() {
    private var onValueChange: (String?) -> Unit = {}
    private var lastValue: String? = null

    private val textHolder = UIBlock().constrain {
        width = ChildBasedSizeConstraint() + 6.pixels
        height = RelativeConstraint()
        color = VigilancePalette.getDarkDivider().toConstraint()
    } childOf this effect OutlineEffect(VigilancePalette.getComponentBorder(), 1f)

    private val textInput: AbstractTextInput = UITextInput(placeholder = placeholder)
        .setMinWidth(50.pixels)
        .setMaxWidth(basicWidthConstraint { this.parent.getWidth() * 0.5f })
        .constrain {
            x = 3.pixels
            y = CenterConstraint()
            color = VigilancePalette.getText().toConstraint()
        }

    private var hasSetInitialText = false

    init {
        textInput childOf textHolder
        textInput.onUpdate { newText ->
            changeValue(newText)
        }.onLeftClick { event ->
            event.stopPropagation()

            textInput.grabWindowFocus()
        }.onFocus {
            textInput.setActive(true)
        }.onFocusLost {
            textInput.setActive(false)
        }

        constrain {
            width = ChildBasedSizeConstraint()
            height = 15.pixels
        }
    }

    override fun animationFrame() {
        super.animationFrame()

        if (!hasSetInitialText) {
            textInput.setText(initial)
            hasSetInitialText = true
        }
    }

    fun closePopups(instantly: Boolean) {
        textInput.setActive(false)
    }

    fun onValueChange(listener: (String?) -> Unit) {
        this.onValueChange = listener
    }

    fun changeValue(newValue: String?, callListener: Boolean = true) {
        if (newValue != lastValue) {
            lastValue = newValue
            this.onValueChange(newValue)
        }
    }
}