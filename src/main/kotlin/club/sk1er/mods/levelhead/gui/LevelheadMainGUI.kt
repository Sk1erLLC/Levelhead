package club.sk1er.mods.levelhead.gui

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.display.*
import club.sk1er.mods.levelhead.gui.components.ChatPreviewComponent
import club.sk1er.mods.levelhead.gui.components.TabPreviewComponent
import com.mojang.authlib.GameProfile
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.api.gui.buildConfirmationModal
import gg.essential.api.gui.buildEmulatedPlayer
import gg.essential.api.utils.JsonHolder
import gg.essential.api.utils.Multithreading
import gg.essential.api.utils.WebUtil.fetchJSON
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.ChatColor
import gg.essential.universal.UDesktop
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import java.awt.Color
import java.net.URI

class LevelheadMainGUI : EssentialGUI("§lLevelhead §r§8by Sk1er LLC") {

    private val instance: Levelhead = Levelhead.INSTANCE

    private val masterToggle = SwitchComponent(instance.displayManager.masterConfig.isEnabled).constrain {
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

    private val credits = UIText("Remaining credits: ${instance.rawPurchases.optInt("remaining_levelhead_credits")}").constrain {
        x = 30.percent() + 5.pixels()
        y = 11.pixels()
    } childOf titleBar

    private var display = aboveHeadDisplay().constrain {
        x = 0.pixels()
        width = RelativeConstraint()
        height = FillConstraint(false)
    } childOf content

    init {
        masterToggle.onValueChange {
            instance.displayManager.masterConfig.isEnabled = it as Boolean
        }

        editing.onValueChange {
            updateDisplay(it)
        }


        Inspector(window).constrain {
            x = 10.pixels(true)
            y = 10.pixels(true)
        } childOf window
    }

    private fun updateDisplay(index: Int) {
        display.hide()
        display = when(index) {
            2 -> chatDisplay()
            1 -> tabDisplay()
            else -> aboveHeadDisplay()
        }.constrain {
            x = 0.pixels()
            width = RelativeConstraint()
            height = FillConstraint(false)
        } childOf content
        instance.refreshRawPurchases()
        credits.setText("Remaining credits: ${instance.rawPurchases.optInt("remaining_levelhead_credits")}")

    }

    private inner class LevelheadContainer : UIComponent() {
        var title: String = ""
            set(value) {
                titleText.setText(value)
                field = value
            }

        val displayContainer by UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 30.percent()
            height = RelativeConstraint()
        } childOf this

        private val divider by UIBlock(VigilancePalette.getDivider()).constrain {
            x = SiblingConstraint()
            y = CramSiblingConstraint()
            width = 1.pixel()
            height = componentHeightConstraint(displayContainer)
        } childOf this

        val settingsContainer by UIContainer().constrain {
            x = SiblingConstraint()
            y = 0.pixels()
            width = 70.percent() - 1.pixel()
            height = componentHeightConstraint(displayContainer)
        } childOf this

        private val titleText by UIText().constrain {
            x = 10.pixels()
            y = 7.5.pixels()
            textScale = 2.5.pixels()
        } childOf settingsContainer

        private val underline = UIBlock(VigilancePalette.getDivider()).constrain {
            x = 10.pixels()
            y = SiblingConstraint(2.5f)
            width = RelativeConstraint() - 20.pixels()
            height = 1.pixel()
        } childOf settingsContainer

        private val scrollContainer = UIContainer().constrain {
            x = 10.pixels()
            y = SiblingConstraint(2.5f)
            width = RelativeConstraint() - 20.pixels()
            height = FillConstraint(false) - 2.5.pixels()
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
        }

    }

    private fun buildLevelheadContainer(block: LevelheadContainer.() -> Unit): LevelheadContainer =
        LevelheadContainer().apply(block)

    private fun aboveHeadDisplay() = buildLevelheadContainer {
        title = "Above Head"

        val aboveHead = instance.displayManager.aboveHead

        val player by EssentialAPI.getEssentialComponentFactory().buildEmulatedPlayer {
            profile = GameProfile(UPlayer.getUUID(), UPlayer.getPlayer()!!.displayName.formattedText)
        }.constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 60.percent()
        } childOf displayContainer

        val purchase by ButtonComponent("Purchase more layers") {
            attemptPurchase("head")
        }.constrain {
            x = 10.pixels(alignOpposite = true)
            y = 5.pixels()
        } childOf settingsContainer

        val aboveHeadPurchases = instance.levelheadPurchaseStates.extraHead
        aboveHead.forEachIndexed { index, aboveHeadDisplay ->
            if (index > aboveHeadPurchases) return@forEachIndexed

            aboveHeadDisplay.createComponent(aboveHead).constrain {
                y = SiblingConstraint()
            } childOf settings
        }
    }

    private fun chatDisplay() = buildLevelheadContainer {
        title = "Chat"

        val chat: ChatDisplay = instance.displayManager.chat as ChatDisplay

        val preview = ChatPreviewComponent(
            "${UPlayer.getPlayer()!!.name}§r: Hi!",
            chat.config.type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
            chat.config
        ).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf displayContainer

        if (instance.levelheadPurchaseStates.isChat) {
            chat.config.createComponents(settings, preview)
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

    private fun tabDisplay() = buildLevelheadContainer {
        title = "Tab"

        val tab = instance.displayManager.tab

        val tabDisplay = TabPreviewComponent(UPlayer).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        } childOf displayContainer

        if (instance.levelheadPurchaseStates.isTab) {
            tab.config.createComponents(settings, tab)
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

    private fun AboveHeadDisplay.createComponent(aboveHead: List<AboveHeadDisplay>): UIComponent {
        val container = UIContainer().constrain {
            width = RelativeConstraint()
            height = AspectConstraint(0.4f)
        }
        val text = UIText("§nLayer ${aboveHead.indexOf(this)}").constrain {
            x = 0.pixels()
            y = 0.pixels()
        } childOf container
        val content = UIContainer().constrain {
            y = SiblingConstraint()
            height = RelativeConstraint()
            width = RelativeConstraint()

        } childOf container
        this.config.createComponents(content, this)

        return container
    }

    private fun DisplayConfig.createComponents(parent: UIComponent, preview: ChatPreviewComponent) {
        val divider = UIBlock(VigilancePalette.getDivider()).constrain {
            x = RelativeConstraint(0.5f)
            y = SiblingConstraint(5f)
            width = 1.pixel()
            height = RelativeConstraint()
        } childOf parent
        val showLabel = UIText("Show on self").constrain {
            x = 0.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf parent
        val showToggle = SwitchComponent(this.isShowSelf).constrain {
            x = 5.75.pixels(true).to(divider) as XConstraint
            y = CramSiblingConstraint() - 0.5.pixels()
        } childOf parent
        showToggle.onValueChange {
            this.isShowSelf = it as Boolean
            preview.update()
        }
        val typeLabel = UIText("Type: ").constrain {
            x = 5.pixels().to(divider) as XConstraint
            y = CramSiblingConstraint() + 0.5.pixels()
        } childOf parent
        val typeOptions = instance.allowedTypes()
        val type = DropDown(
            typeOptions.keys.sortedBy { it }.indexOf(this.type),
            typeOptions.values.toList().sortedBy { it }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf parent
        type.onValueChange { value ->
            this.type = typeOptions.keys.sortedBy { string -> string }.toList()[value]
            println(typeOptions)
            println(this.type)
            preview.stat = this.type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }
        }
        val colorOptions = ChatColor.values().filter { it.isColor() }
        val bracketLabel = UIText("Bracket Color").constrain {
            x = 0.pixels()
            y = SiblingConstraint().to(showLabel) + 15.pixels()
        } childOf parent
        val bracketColor = DropDown(
            colorOptions.indexOf(
                colorOptions.find {
                    it.toString() == this.headerColor
                }
            ),
            colorOptions.map { "${it}${it.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}" }
        ).constrain {
            x = 5.pixels(true).to(divider) as XConstraint
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf parent
        bracketColor.onValueChange {
            this.headerColor = colorOptions[it].toString().drop(1)
            preview.update()
        }
        val textLabel = UIText("Text Color").constrain {
            x = 5.pixels().to(divider) as XConstraint
            y = CramSiblingConstraint() + 4.5.pixels()
        } childOf parent
        val textColor = DropDown(
            colorOptions.indexOf(
                colorOptions.find {
                    it.toString() == this.footerColor
                }
            ),
            colorOptions.map { "${it}${it.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}" }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf parent
        textColor.onValueChange {
            this.footerColor = colorOptions[it].toString().drop(1)
            preview.update()
        }
    }

    private fun DisplayConfig.createComponents(parent: UIComponent, display: LevelheadDisplay) {
        val leftContainer = UIContainer().constrain {
            width = RelativeConstraint(0.5f) - 0.5.pixels()
            height = ChildBasedRangeConstraint()
        } childOf parent
        val rightContainer = UIContainer().constrain {
            x = 0.pixels(true)
            width = RelativeConstraint(0.5f) - 0.5.pixels()
            height = ChildBasedRangeConstraint()
        } childOf parent
        val divider = UIBlock(VigilancePalette.getDivider()).constrain {
            x = RelativeConstraint(0.5f) - 0.5.pixels()
            width = 1.pixel()
            height = MaxConstraint(leftContainer.constraints.height, rightContainer.constraints.height)
        } childOf parent
        val showLabel = UIText("Show on self").constrain {
            x = 0.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf leftContainer
        val showToggle = SwitchComponent(this.isShowSelf).constrain {
            x = 5.75.pixels(true)
            y = CramSiblingConstraint() - 0.5.pixels()
        } childOf leftContainer
        showToggle.onValueChange {
            this.isShowSelf = it as Boolean
        }
        val typeLabel = UIText("Type: ").constrain {
            x = 5.pixels()
            y = CramSiblingConstraint() + 5.5.pixels()
        } childOf rightContainer
        val options = instance.allowedTypes()
        val type = DropDown(
            options.keys.sortedBy { it }.indexOf(this.type),
            options.values.toList().sortedBy { it }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() - 4.5.pixels()
        } childOf rightContainer
        type.onValueChange {
            this.type = options.keys.sortedBy { string -> string }.toList()[it]
            display.update()
        }
        if (display is AboveHeadDisplay) {
            val textLabel = UIText("Prefix: ").constrain {
                x = 0.pixels()
                y = SiblingConstraint(5f).to(showToggle) as YConstraint
            } childOf leftContainer
            val textInput = TextComponent(this.customHeader, "", false, false).constrain {
                x = 6.pixels(true)
                y = CramSiblingConstraint()
            } childOf leftContainer
            textInput.onValueChange {
                if (it !is String) return@onValueChange
                this.customHeader = it
            }
        }
        val header = ColorSetting(this, true, display).constrain {
            x = 0.pixels()
            y = SiblingConstraint() + 5.pixels()
            width = RelativeConstraint() - 5.pixels()
            height = AspectConstraint()
        } childOf leftContainer
        if (display !is TabDisplay)
            ColorSetting(this, false, display).constrain {
                x = 5.pixels()
                y = CopyConstraintFloat().to(header) as YConstraint
                width = RelativeConstraint() - 10.pixels()
                height = AspectConstraint()
            } childOf rightContainer


    }

    private class ColorSetting(val config: DisplayConfig, val header: Boolean, val display: LevelheadDisplay): UIComponent() {
        val colorLabel = UIText(if (header) "Header Color:" else "Footer Color:").constrain {
            x = 0.pixels()
            y = 4.5.pixels()
        } childOf this
        val options = listOf("Chroma", "RGB") + ChatColor.values().filter { it.isColor() }
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
                when(it) {
                    0 -> {
                        if (header) {
                            config.isHeaderRgb = false
                            config.isHeaderChroma = true
                        } else {
                            config.isFooterRgb = false
                            config.isFooterChroma = true
                        }
                        updateSelector()
                    }
                    1 -> {
                        if (header) {
                            config.isHeaderRgb = true
                            config.isHeaderChroma = false
                        } else {
                            config.isFooterRgb = true
                            config.isFooterChroma = false
                        }
                        updateSelector()
                    }
                    else -> {
                        if (header) {
                            config.isHeaderRgb = false
                            config.isHeaderChroma = false
                            config.headerColor = options[it].toString().drop(1)
                            println(options[it].toString().drop(1))
                            config.headerRed = (options[it] as ChatColor).color!!.red
                            config.headerGreen = (options[it] as ChatColor).color!!.green
                            config.headerBlue = (options[it] as ChatColor).color!!.blue
                        } else {
                            config.isFooterRgb = false
                            config.isFooterChroma = false
                            config.footerColor = options[it].toString().drop(1)
                            config.footerRed = (options[it] as ChatColor).color!!.red
                            config.footerGreen = (options[it] as ChatColor).color!!.green
                            config.footerBlue = (options[it] as ChatColor).color!!.blue
                        }
                        updateSelector()
                    }
                }
                display.update()
            }
        }
        var selector = generateSetting().constrain {
            x = 0.pixels(true)
            y = SiblingConstraint(10f).to(colorLabel) as YConstraint
        } childOf this

        private fun updateSelector() {
            selector.hide()
            selector = generateSetting().constrain {
                x = 0.pixels(true)
                y = SiblingConstraint(10f).to(colorLabel) as YConstraint
            } childOf this
        }

        private fun getCurrentSetting() = when(config.getMode(header)) {
            "Chat Color" -> {
                ChatColor.values().filter { it.isColor() }
                    .find {
                        it.toString() == if (header) config.headerColor else config.footerColor
                    }
            }
            else -> config.getMode(header)
        }

        private fun generateSetting(): UIComponent = when (config.getMode(header)) {
            "Chroma" -> UIBlock(Color.RED).constrain {
                color = basicColorConstraint {
                    val time = System.currentTimeMillis()
                    val z = 1000.0f
                    Color(Color.HSBtoRGB((time % z.toInt()).toFloat() / z, 0.8f, 0.8f))
                }
                width = RelativeConstraint(0.55f)
                height = AspectConstraint(0.8f)
            }
            else -> {
                val picker = if (header) {
                    ColorPicker(
                        Color(
                            config.headerRed,
                            config.headerGreen,
                            config.headerBlue
                        ), false
                    )
                } else {
                    ColorPicker(
                        Color(
                            config.footerRed,
                            config.footerGreen,
                            config.footerBlue
                        ), false
                    )
                }.constrain {
                    width = RelativeConstraint(0.55f)
                    height = AspectConstraint(0.8f)
                }
                picker.onValueChange {
                    if (header) {
                        config.isHeaderChroma = false
                        config.isHeaderRgb = true
                        config.headerRed = it.red
                        config.headerGreen = it.green
                        config.headerBlue = it.blue
                    } else {
                        config.isFooterChroma = false
                        config.isFooterRgb = true
                        config.footerRed = it.red
                        config.footerGreen = it.green
                        config.footerBlue = it.blue
                    }
                    this.color.select(1)
                    display.update()
                }
                picker
            }
        }

        private fun DisplayConfig.getMode(header: Boolean) = if (header) {
            when {
                this.isHeaderChroma -> "Chroma"
                this.isHeaderRgb -> "RGB"
                else -> "Chat Color"
            }
        } else {
            when {
                this.isFooterChroma -> "Chroma"
                this.isFooterRgb -> "RGB"
                else -> "Chat Color"
            }
        }
    }







    private fun attemptPurchase(type: String){
        val paidData = instance.paidData
        val extraDisplays = paidData.optJSONObject("extra_displays")
        val stats = paidData.optJSONObject("stats")

        val seed: JsonHolder = when {
            extraDisplays.has(type) -> {
                extraDisplays.optJSONObject(type)
            }
            stats.has(type) -> {
                stats.optJSONObject(type)
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

        val remainingCredits = instance.rawPurchases.optInt("remaining_levelhead_credits")
        when {
            remainingCredits < seed.optInt("cost") -> {
                EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                    text = """
                        # Insufficient credits! ${seed.optString("name")} costs ${seed.optInt("cost")} credits
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
            instance.auth.isFailed -> {
                EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                    text = "Could not verify your identity. Please restart the client."
                    secondaryText = "If issues persist, contact Sk1er"
                    confirmButtonText = "Close"
                    denyButtonText = ""
                } childOf window
            }
            else -> {
                val name = seed.optString("name")
                EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                    text = "You are about to purchase package ${name}."
                    onConfirm = {
                        Multithreading.submit {
                            val jsonHolder =
                                fetchJSON("https://api.sk1er.club/levelhead_purchase?access_token=" + instance.auth.accessKey + "&request=" + type + "&hash=" + instance.auth.hash)
                            if (jsonHolder.optBoolean("success")) {
                                instance.refreshPurchaseStates()
                                EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                    text = "Successfully purchased package ${name}."
                                    confirmButtonText = "Close"
                                    onConfirm = {
                                        updateDisplay(editing.getValue())
                                    }
                                    denyButtonText = ""
                                } childOf window
                            } else {
                                EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                    text = "Failed to purchase package ${name}."
                                    secondaryText = "Cause: ${jsonHolder.optString("cause")}"
                                    confirmButtonText = "Close"
                                    denyButtonText = ""
                                    onConfirm = {
                                        updateDisplay(editing.getValue())
                                    }
                                } childOf window
                            }
                        }

                    }
                } childOf window
            }
        }
    }
}

fun LevelheadDisplay.update() {
    this.cache.remove(UPlayer.getUUID())
    this.trueValueCache.remove(UPlayer.getUUID())
}