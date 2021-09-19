package club.sk1er.mods.levelhead.gui.components

import club.sk1er.mods.levelhead.display.DisplayConfig
import club.sk1er.mods.levelhead.renderer.LevelheadChatRenderer
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.ChildBasedSizeConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.constrain
import gg.essential.universal.wrappers.message.UTextComponent
import java.awt.Color

class ChatPreviewComponent(private val previewMessage: String, stat: String, val config: DisplayConfig) : UIBlock(Color(Int.MIN_VALUE)) {
    var stat: String = stat
        set(value) {
            field = value
            text = LevelheadChatRenderer.modifyChat(
                UTextComponent(previewMessage).component,
                value,
                config
            ).formattedText
        }
    private var text: String = LevelheadChatRenderer.modifyChat(
        UTextComponent(previewMessage).component,
        stat,
        config
    ).formattedText
        set(value) {
            field = value
            textComponent.setText(value)
        }

    private val textComponent = UIText(text) childOf this

    init {
        this.constrain {
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        }
    }

    fun update() {
        text = LevelheadChatRenderer.modifyChat(
            UTextComponent(previewMessage).component,
            stat,
            config
        ).formattedText
    }
}