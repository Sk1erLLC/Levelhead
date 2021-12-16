package club.sk1er.mods.levelhead.gui.components

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.render.ChatRender
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.*
import gg.essential.universal.wrappers.message.UTextComponent
import java.awt.Color

class ChatPreviewComponent(private val previewMessage: String) : LevelheadPreviewComponent() {

    private val background = UIBlock(Color(Int.MIN_VALUE)).constrain {
        width = ChildBasedSizeConstraint()
        height = ChildBasedSizeConstraint()
    } childOf this

    var config = Levelhead.displayManager.chat.config

    val stat: String
        get() = Levelhead.displayManager.chat.config.type.split('_').joinToString (separator = " ") {
            it.lowercase().replaceFirstChar { firstChar ->
                firstChar.uppercase()
            }
        }

    private var text: String = ChatRender.modifyChat(
        UTextComponent(previewMessage).component,
        stat,
        config
    ).formattedText
        set(value) {
            field = value
            textComponent.setText(value)
        }

    private val textComponent = UIText(text).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    } childOf this

    init {
        this.constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        }
    }

    override fun update() {
        text = ChatRender.modifyChat(
            UTextComponent(previewMessage).component,
            stat,
            config
        ).formattedText
    }
}