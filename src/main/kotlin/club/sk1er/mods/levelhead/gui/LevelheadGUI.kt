package club.sk1er.mods.levelhead.gui

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.tryToGetChatColor
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.update
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.ChatDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import club.sk1er.mods.levelhead.display.TabDisplay
import club.sk1er.mods.levelhead.gui.components.AboveHeadPreviewComponent
import club.sk1er.mods.levelhead.gui.components.ChatPreviewComponent
import club.sk1er.mods.levelhead.gui.components.LevelheadPreviewComponent
import club.sk1er.mods.levelhead.gui.components.TabPreviewComponent
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.ChatColor
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import java.awt.Color

@Suppress("unused")
class LevelheadGUI : EssentialGUI("§lLevelhead §r§8by Sk1er LLC") {

    override fun onScreenClose() {
        Levelhead.displayManager.saveConfig()
        super.onScreenClose()
    }

    private val masterToggle = SwitchComponent(Levelhead.displayManager.config.enabled).constrain {
        x = 10.pixels(true)
        y = 10.pixels()
    } childOf titleBar

    private val masterLabel = UIText("§7Master Toggle").constrain {
        x = (masterToggle.getWidth() + 5).pixels(true).to(masterToggle) as XConstraint
        y = 11.pixels()
    } childOf titleBar

    private val editing = DropDown(0, listOf("Head", "Tab", "Chat")).constrain {
        x = (masterLabel.getWidth() + 10).pixels(true).to(masterLabel) as XConstraint
        y = 5.pixels()
    } childOf titleBar

    private val credits = UIText("Remaining credits: ${Levelhead.rawPurchases["remaining_levelhead_credits"].asInt}").constrain {
        x = 30.percent() + 5.pixels()
        y = 11.pixels()
    } childOf titleBar

    private val aboveHead by lazy {
        levelheadContainer {
            title = "Above Head"

            preview = AboveHeadPreviewComponent()

            Levelhead.displayManager.aboveHead.forEachIndexed { i, display ->
                val container = UIContainer().constrain {
                    width = RelativeConstraint()
                    height = ChildBasedRangeConstraint()
                }
                val text = UIText("§nLayer ${i + 1}").constrain {
                    x = 0.pixels()
                    y = 0.pixels()
                } childOf container
                val content = UIContainer().constrain {
                    y = SiblingConstraint()
                    height = ChildBasedRangeConstraint()
                    width = RelativeConstraint()
                } childOf container
                content.createComponents(display.config, display)
                if (i < Levelhead.displayManager.aboveHead.size) {
                    val funnyLongDivider = UIBlock(VigilancePalette.getDivider()).constrain {
                        x = 50.percent - 0.5.pixels
                        y = content.constraints.y
                        width = 1.pixel
                        height = content.constraints.height + 20.pixels
                    } childOf container

                    container.constraints.height -= 20.pixels
                }
                container.constrain {
                    y = SiblingConstraint()
                } childOf settings
            }
        }
    }

    private val chat by lazy {
        levelheadContainer {
            title = "Chat"

            preview = ChatPreviewComponent(
                "${UPlayer.getPlayer()?.name ?: "Sk1er"}§r: Hi!"
            )

            Levelhead.displayManager.chat.run {
                settings.createComponents(this.config, this)
            }
        }
    }

    private val tab by lazy {
        levelheadContainer {
            title = "Tab"

            preview = TabPreviewComponent()

            Levelhead.displayManager.tab.run {
                settings.createComponents(this.config, this)
            }
        }
    }

    private var container: LevelheadContainer = aboveHead childOf content
        set(value) {
            container.hide()
            value.childOf(content)
            field = value
        }

    init {
        masterToggle.onValueChange {
            Levelhead.displayManager.config.enabled = it as Boolean
        }

        editing.onValueChange {
            container = when (it) {
                2 -> chat
                1 -> tab
                else -> aboveHead
            }
        }


        if (EssentialAPI.getMinecraftUtil().isDevelopment()) {
            Inspector(window).constrain {
                x = 10.pixels(true)
                y = 10.pixels(true)
            } childOf window
        }
    }

    private inner class LevelheadContainer : UIComponent() {
        var title: String = ""
            set(value) {
                titleText.setText(value)
                field = value
            }

        private val previewContainer by UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent
            height = 40.percent
        } childOf this

        var preview: LevelheadPreviewComponent? = null
            set(value) {
                field = value.also {
                    it?.constrain {
                        x = CenterConstraint()
                        y = CenterConstraint()
                    }?.childOf(previewContainer)
                    it?.effect(ScissorEffect())
                }
            }

        private val divider by UIBlock(VigilancePalette.getDivider()).constrain {
            x = 0.pixels
            y = SiblingConstraint()
            width = RelativeConstraint()
            height = 1.pixel
        } childOf this

        val settingsContainer by UIContainer().constrain {
            x = 0.pixels
            y = SiblingConstraint()
            width = RelativeConstraint()
            height = 60.percent()
        } childOf this

        private val titleText by UIText(shadow = false).constrain {
            x = 10.pixels()
            y = 7.5.pixels(true).to(divider) as YConstraint
            textScale = 2.5.pixels()
        } childOf this

        private val scrollContainer = UIContainer().constrain {
            x = 5.pixels
            y = 5.pixels
            width = RelativeConstraint()
            height = FillConstraint(false)
        } childOf settingsContainer

        val scrollBar = UIBlock(VigilancePalette.getScrollBar()).constrain {
            x = 98.5.percent
            width = 1.percent()
        } childOf scrollContainer

        val settings = ScrollComponent().constrain {
            width = 98.percent
            height = 100.percent
        } childOf scrollContainer

        init {
            settings.setVerticalScrollBarComponent(scrollBar)

            constrain {
                width = RelativeConstraint()
                height = FillConstraint(false)
            }
        }
    }

    private fun UIComponent.createComponents(config: DisplayConfig, display: LevelheadDisplay) {
        val leftContainer = UIContainer().constrain {
            width = RelativeConstraint(0.5f) - 0.5.pixels()
            height = ChildBasedRangeConstraint()
        } childOf this
        val rightContainer = UIContainer().constrain {
            x = 0.pixels(true)
            width = RelativeConstraint(0.5f) - 0.5.pixels()
            height = ChildBasedRangeConstraint()
        } childOf this
        val divider = UIBlock(VigilancePalette.getDivider()).constrain {
            x = RelativeConstraint(0.5f) - 0.5.pixels()
            width = 1.pixel()
            height = MaxConstraint(leftContainer.constraints.height, rightContainer.constraints.height)
        } childOf this
        val showLabel = UIText("Show on self").constrain {
            x = 2.5.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf leftContainer
        val showToggle = SwitchComponent(config.showSelf).constrain {
            x = 5.75.pixels(true)
            y = CramSiblingConstraint() - 0.5.pixels()
        } childOf leftContainer
        showToggle.onValueChange {
            config.showSelf = it as Boolean
        }
        val typeLabel = UIText("Type: ").constrain {
            x = 5.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf rightContainer
        val options = Levelhead.types
        val type = DropDown(
            options.entrySet().map { it.key }.sortedBy { it }.indexOf(config.type),
            options.entrySet().map { it.value.asJsonObject["name"].asString }.sortedBy { it }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf rightContainer
        type.onValueChange {
            config.type = options.entrySet().map { it.key }.sortedBy { string -> string }[it]
            display.update()
        }
        if (display is AboveHeadDisplay) {
            val textLabel = UIText("Prefix: ").constrain {
                x = 2.5.pixels()
                y = SiblingConstraint(5f).to(showToggle) as YConstraint
            } childOf leftContainer
            val textInput = TextComponent(config.headerString, "", false, false).constrain {
                x = 6.pixels(true)
                y = CramSiblingConstraint()
            } childOf leftContainer
            textInput.onValueChange {
                if (it !is String) return@onValueChange
                config.headerString = it
                display.update()
            }
        }
        val header = ColorSetting(config, true, display).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(10f)
            width = RelativeConstraint() - 10.pixels()
            height = AspectConstraint(0.4f)
        } childOf leftContainer
        if (display !is TabDisplay)
            ColorSetting(config, false, display).constrain {
                x = CenterConstraint()
                y = CopyConstraintFloat().to(header) as YConstraint
                width = RelativeConstraint() - 10.pixels()
                height = AspectConstraint(0.4f)
            } childOf rightContainer
    }

    private class ColorSetting(val config: DisplayConfig, val header: Boolean, val display: LevelheadDisplay): UIComponent() {
        val colorLabel = UIText(if (header) "Header Color:" else "Footer Color:").constrain {
            x = 0.pixels()
            y = 4.5.pixels()
        } childOf this
        val options = (if (display !is ChatDisplay) listOf("Chroma", "RGB") else listOf<Any>()) + ChatColor.values().filter { it.isColor() }
        val color = DropDown(
            options.indexOf(getCurrentSetting()),
            options.map {
                if (it is ChatColor)
                    "${it}${it.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}"
                else it.toString()
            }
        ).constrain {
            x = 0.pixels(true)
            y = 0.pixels()
        } childOf this
        init {
            color.onValueChange {
                when {
                    it == 0 && display !is ChatDisplay -> {
                        if (header) {
                            config.headerChroma = true
                        } else {
                            config.footerChroma = true
                        }
                    }
                    it == 1 && display !is ChatDisplay -> {
                        if (header) {
                            config.headerChroma = false
                        } else {
                            config.footerChroma = false
                        }
                    }
                    else -> {
                        if (header) {
                            config.headerChroma = false
                            config.headerColor = (options[it] as ChatColor).color!!
                            if (selector.getCurrentColor() == config.headerColor) return@onValueChange
                            val (red, green, blue) = config.headerColor
                            val (h,s,b) = Color.RGBtoHSB(red, green, blue, null)
                            selector.setHSB(h, s, b)
                        } else {
                            config.footerChroma = false
                            config.footerColor = (options[it] as ChatColor).color!!
                            if (selector.getCurrentColor() == config.footerColor) return@onValueChange
                            val (red, green, blue) = config.footerColor
                            val (h,s,b) = Color.RGBtoHSB(red, green, blue, null)
                            selector.setHSB(h, s, b)
                        }
                    }
                }
                display.update()
            }
        }
        var selector = if (header) {
            ColorPicker(
                config.headerColor, false
            )
        } else {
            ColorPicker(
                config.footerColor, false
            )
        }.constrain {
            x = CenterConstraint()
            y = SiblingConstraint(10f).to(colorLabel) as YConstraint
            width = AspectConstraint(1.25f)
            height = 20.percentOfWindow.coerceAtLeast(30.percent)
        }.childOf(this).also {
            it.onValueChange {
                if (header) {
                    config.headerChroma = false
                    config.headerColor = it
                } else {
                    config.footerChroma = false
                    config.footerColor = it
                }
                this.color.select(it.tryToGetChatColor()?.let { options.indexOf(it) } ?: 1)
                display.update()
            }

        }

        private fun getCurrentSetting() = when(config.getMode(header)) {
            "Chat Color" -> {
                ChatColor.values().filter { it.isColor() }
                    .find {
                        it.color!!.rgb == if (header) config.headerColor.rgb else config.footerColor.rgb
                    }
            }
            else -> config.getMode(header)
        }

        private fun DisplayConfig.getMode(header: Boolean) = if (header) {
            when {
                this.headerChroma -> "Chroma"
                this.headerColor.tryToGetChatColor() != null -> "Chat Color"
                else -> "RGB"
            }
        } else {
            when {
                this.footerChroma -> "Chroma"
                this.footerColor.tryToGetChatColor() != null -> "Chat Color"
                else -> "RGB"
            }
        }

        private fun LevelheadDisplay.update() {
            this.cache.remove(UPlayer.getUUID())
            Levelhead.fetch(UPlayer.getUUID(), this, if (this is AboveHeadDisplay) this.bottomValue else false)
        }
    }


    private fun levelheadContainer(block: LevelheadContainer.() -> Unit): LevelheadContainer =
        LevelheadContainer().apply(block)
}