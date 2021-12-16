package club.sk1er.mods.levelhead.gui

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.Levelhead.rawWithAgent
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.invalidateableLazy
import club.sk1er.mods.levelhead.core.tryToGetChatColor
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
import gg.essential.api.gui.buildConfirmationModal
import gg.essential.api.utils.Multithreading
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.ChatColor
import gg.essential.universal.UDesktop
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import java.awt.Color
import java.net.URI

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
        x = CenterConstraint()
        y = 11.pixels()
    } childOf titleBar

    private val aboveHead by lazy {
        levelheadContainer {
            title = "Above Head"

            preview = AboveHeadPreviewComponent()

            Levelhead.displayManager.aboveHead.forEachIndexed { i, display ->
                if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
                val container = UIContainer().constrain {
                    width = RelativeConstraint()
                    height = ChildBasedRangeConstraint()
                }
                val toggle = SwitchComponent(display.config.enabled).constrain {
                    x = 2.5.pixels(alignOpposite = true)
                    y = 2.5.pixels()

                } childOf container
                toggle.onValueChange {
                    display.config.enabled = it as Boolean
                }
                val text = UIText("§nLayer ${i + 1}").constrain {
                    x = 0.pixels()
                    y = CenterConstraint() boundTo toggle
                } childOf container
                val content = UIContainer().constrain {
                    y = SiblingConstraint(2.5f)
                    height = ChildBasedRangeConstraint()
                    width = RelativeConstraint()
                } childOf container
                content.createComponents(display, preview!!)
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

            ButtonComponent("Purchase more layers") {
                attemptPurchase("head")
            }.constrain {
                x = 10.pixels(true)
                y = 7.5.pixels(true).to(divider) as YConstraint
                textScale = 2.5.pixels()
            } childOf this
        }
    }

    private val chatDelegate = invalidateableLazy {
        levelheadContainer {
            title = "Chat"

            preview = ChatPreviewComponent(
                "${UPlayer.getPlayer()?.name ?: "Sk1er"}§r: Hi!"
            )

            if (Levelhead.LevelheadPurchaseStates.chat) {
                Levelhead.displayManager.chat.run {
                    settings.createComponents(this, preview!!)
                    val toggle = SwitchComponent(this.config.enabled).constrain {
                        x = 2.5.pixels(alignOpposite = true)
                        y = CenterConstraint() boundTo titleText
                    } childOf this@levelheadContainer
                    toggle.onValueChange {
                        this.config.enabled = it as Boolean
                        this.update()
                        preview?.update()
                    }
                }
            } else {
                val text = UIText("Levelhead Chat Display not purchased!").constrain {
                    x = CenterConstraint()
                } childOf settings
                ButtonComponent("Purchase Chat Display") {
                    attemptPurchase("chat")
                    this.hide()
                }.constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint(5f)
                } childOf settings
            }
        }
    }

    private var chat by chatDelegate

    private val tabDelegate = invalidateableLazy {
        levelheadContainer {
            title = "Tab"

            preview = TabPreviewComponent()

            if (Levelhead.LevelheadPurchaseStates.tab) {
                Levelhead.displayManager.tab.run {
                    settings.createComponents(this, preview!!)
                    val toggle = SwitchComponent(this.config.enabled).constrain {
                        x = 2.5.pixels(alignOpposite = true)
                        y = CenterConstraint() boundTo titleText
                    } childOf this@levelheadContainer
                    toggle.onValueChange {
                        this.config.enabled = it as Boolean
                        this.update()
                        preview?.update()
                    }
                }
            } else {
                val text = UIText("Levelhead Tab Display not purchased!").constrain {
                    x = CenterConstraint()
                } childOf settings
                ButtonComponent("Purchase Tab Display") {
                    attemptPurchase("tab")
                    this.hide()
                }.constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint(5f)
                } childOf settings
            }
        }
    }

    private var tab by tabDelegate

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

        val divider by UIBlock(VigilancePalette.getDivider()).constrain {
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

        val titleText by UIText(shadow = false).constrain {
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

        fun attemptPurchase(type: String){
            val paidData = Levelhead.paidData
            val extraDisplays = paidData["extra_displays"].asJsonObject
            val stats = paidData["stats"].asJsonObject

            val seed = when {
                extraDisplays.has(type) -> {
                    extraDisplays[type].asJsonObject
                }
                stats.has(type) -> {
                    stats[type].asJsonObject
                }
                else -> {
                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                        text = "Could not find package: ${type}."
                        secondaryText = "Please contact Sk1er immediately."
                        confirmButtonText = "Close"
                        denyButtonText = ""
                    } childOf window

                    return
                }
            }

            val remainingCredits = Levelhead.rawPurchases["remaining_levelhead_credits"].asInt
            when {
                remainingCredits < seed["cost"].asInt -> {
                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                        text = """
                        # Insufficient credits! ${seed["name"].asString} costs ${seed["cost"].asInt} credits
                        # but you only have ${remainingCredits}.
                        # You can purchase more credits here: https://purchase.sk1er.club/category/1050972
                    """.trimMargin("#")
                        confirmButtonText = "Purchase more credits"
                        onConfirm = {
                            UDesktop.browse(URI.create("https://purchase.sk1er.club/category/1050972"))
                        }
                        denyButtonText = "Close"
                    } childOf window
                    return
                }
                Levelhead.auth.isFailed -> {
                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                        text = "Could not verify your identity. Please restart the client."
                        secondaryText = "If issues persist, contact Sk1er"
                        confirmButtonText = "Close"
                        denyButtonText = ""
                    } childOf window
                }
                else -> {
                    val name = seed["name"].asString
                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                        text = "You are about to purchase package ${name}."
                        onConfirm = {
                            Multithreading.submit {
                                val jsonHolder =
                                    jsonParser.parse(rawWithAgent("https://api.sk1er.club/levelhead_purchase?access_token=" + Levelhead.auth.accessKey + "&request=" + type + "&hash=" + Levelhead.auth.hash)).asJsonObject
                                if (jsonHolder["success"].asBoolean) {
                                    Levelhead.refreshPurchaseStates()
                                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                        text = "Successfully purchased package ${name}."
                                        confirmButtonText = "Close"
                                        onConfirm = {
                                            container = when (editing.getValue()) {
                                                2 -> { chatDelegate.invalidate(); chat}
                                                1 -> { tabDelegate.invalidate(); tab }
                                                else -> aboveHead
                                            }
                                        }
                                        denyButtonText = ""
                                    } childOf window
                                } else {
                                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                        text = "Failed to purchase package ${name}."
                                        secondaryText = "Cause: ${jsonHolder["cause"].asString}"
                                        confirmButtonText = "Close"
                                        denyButtonText = ""
                                        onConfirm = {
                                            container = when (editing.getValue()) {
                                                2 -> { chatDelegate.invalidate(); chat}
                                                1 -> { tabDelegate.invalidate(); tab }
                                                else -> aboveHead
                                            }
                                        }
                                    } childOf window
                                }
                            }

                        }
                    } childOf window
                }
            }
            Levelhead.refreshRawPurchases()
            credits.setText("Remaining credits: ${Levelhead.rawPurchases["remaining_levelhead_credits"].asInt}")
        }
    }

    private fun UIComponent.createComponents(display: LevelheadDisplay, preview: LevelheadPreviewComponent) {
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
        val showToggle = SwitchComponent(display.config.showSelf).constrain {
            x = 5.75.pixels(true)
            y = CramSiblingConstraint() - 0.5.pixels()
        } childOf leftContainer
        showToggle.onValueChange {
            display.config.showSelf = it as Boolean
            if (display !is AboveHeadDisplay)
                preview.update()
        }
        val typeLabel = UIText("Type: ").constrain {
            x = 5.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf rightContainer
        val options = Levelhead.allowedTypes
        val type = DropDown(
            options.entrySet().map { it.key }.sortedBy { string -> string }.indexOf(display.config.type).coerceAtLeast(0),
            options.entrySet().sortedBy { it.key }.map { it.value.asJsonObject["name"].asString }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf rightContainer
        type.onValueChange {
            display.config.type = options.entrySet().map { it.key }.sortedBy { string -> string }[it]
            display.update()
            preview.update()
        }
        if (display is AboveHeadDisplay) {
            val textLabel = UIText("Prefix: ").constrain {
                x = 2.5.pixels()
                y = SiblingConstraint(5f).to(showToggle) as YConstraint
            } childOf leftContainer
            val textInput = TextComponent(display.config.headerString, "", false, false).constrain {
                x = 6.pixels(true)
                y = CramSiblingConstraint()
            } childOf leftContainer
            textInput.onValueChange {
                if (it !is String) return@onValueChange
                display.config.headerString = it
                display.update()
            }
        }
        val header = ColorSetting(true, display, preview).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(10f)
            width = RelativeConstraint() - 10.pixels()
            height = AspectConstraint(0.4f)
        } childOf leftContainer
        if (display !is TabDisplay)
            ColorSetting(false, display, preview).constrain {
                x = CenterConstraint()
                y = CopyConstraintFloat().to(header) as YConstraint
                width = RelativeConstraint() - 10.pixels()
                height = AspectConstraint(0.4f)
            } childOf rightContainer
    }

    private class ColorSetting(val header: Boolean, val display: LevelheadDisplay, val preview: LevelheadPreviewComponent): UIComponent() {
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
                            display.config.headerChroma = true
                        } else {
                            display.config.footerChroma = true
                        }
                    }
                    it == 1 && display !is ChatDisplay -> {
                        if (header) {
                            display.config.headerChroma = false
                        } else {
                            display.config.footerChroma = false
                        }
                    }
                    else -> {
                        if (header) {
                            display.config.headerChroma = false
                            display.config.headerColor = (options[it] as ChatColor).color!!
                            if (selector.getCurrentColor() == display.config.headerColor) return@onValueChange
                            val (red, green, blue) = display.config.headerColor
                            val (h,s,b) = Color.RGBtoHSB(red, green, blue, null)
                            selector.setHSB(h, s, b)
                        } else {
                            display.config.footerChroma = false
                            display.config.footerColor = (options[it] as ChatColor).color!!
                            if (selector.getCurrentColor() == display.config.footerColor) return@onValueChange
                            val (red, green, blue) = display.config.footerColor
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
                display.config.headerColor, false
            )
        } else {
            ColorPicker(
                display.config.footerColor, false
            )
        }.constrain {
            x = CenterConstraint()
            y = SiblingConstraint(10f).to(colorLabel) as YConstraint
            width = AspectConstraint(1.25f)
            height = 20.percentOfWindow.coerceAtLeast(30.percent)
        }.childOf(this).also {
            it.onValueChange {
                if (header) {
                    display.config.headerChroma = false
                    display.config.headerColor = it
                } else {
                    display.config.footerChroma = false
                    display.config.footerColor = it
                }
                this.color.select(it.tryToGetChatColor()?.let { options.indexOf(it) } ?: 1)
                display.update()
            }
        }
            set(value) {
                if (display is ChatDisplay) return
                field = value
            }

        init {
            if (display is ChatDisplay) selector.hide()
        }

        private fun getCurrentSetting() = when(display.config.getMode(header)) {
            "Chat Color" -> {
                ChatColor.values().filter { it.isColor() }
                    .find {
                        it.color!!.rgb == if (header) display.config.headerColor.rgb else display.config.footerColor.rgb
                    }
            }
            else -> display.config.getMode(header)
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
            preview.update()
        }
    }


    private fun levelheadContainer(block: LevelheadContainer.() -> Unit): LevelheadContainer =
        LevelheadContainer().apply(block)
}