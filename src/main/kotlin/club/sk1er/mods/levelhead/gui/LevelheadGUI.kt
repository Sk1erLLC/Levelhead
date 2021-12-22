package club.sk1er.mods.levelhead.gui

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.Levelhead.rawWithAgent
import club.sk1er.mods.levelhead.Levelhead.refreshRawPurchases
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.invalidateableLazy
import club.sk1er.mods.levelhead.core.trimmed
import club.sk1er.mods.levelhead.core.tryToGetChatColor
import club.sk1er.mods.levelhead.core.update
import club.sk1er.mods.levelhead.display.*
import club.sk1er.mods.levelhead.gui.components.*
import club.sk1er.mods.levelhead.gui.components.SliderComponent
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.api.gui.buildConfirmationModal
import gg.essential.api.utils.Multithreading
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.ChatColor
import gg.essential.universal.UDesktop
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.ExpandingClickEffect
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.cancelChildren
import java.awt.Color
import java.net.URI

@Suppress("unused")
class LevelheadGUI : EssentialGUI(ElementaVersion.V1, "§lLevelhead §r§8by Sk1er LLC", newGuiScale = EssentialAPI.getGuiUtil().getGuiScale(855), restorePreviousGuiOnClose = false) {

    companion object {
        private var currentPage = 0
    }

    private var screenCloseCallback: () -> Unit = {}
    val initialTypes = (listOf(Levelhead.displayManager.chat, Levelhead.displayManager.tab) + Levelhead.displayManager.aboveHead).associate {
        it to it.config.type
    }

    fun onScreenClose(callback: () -> Unit) {
        screenCloseCallback = callback
    }

    override fun onScreenClose() {
        Levelhead.displayManager.saveConfig()
        screenCloseCallback()
        Levelhead.displayManager.update()
        if (Levelhead.LevelheadPurchaseStates.customLevelhead) Levelhead.displayManager.aboveHead[0].cache[UPlayer.getUUID()] = customTag
        val finalTypes = (listOf(Levelhead.displayManager.chat, Levelhead.displayManager.tab) + Levelhead.displayManager.aboveHead).associate {
            it to it.config.type
        }
        if (finalTypes.any{ initialTypes[it.key] != it.value}) {
            Levelhead.scope.coroutineContext.cancelChildren()
            Levelhead.rateLimiter.resetState()
            Levelhead.displayManager.clearCache()
        }
        super.onScreenClose()
    }

    override fun updateGuiScale() {
        container = when (editing.getValue()) {
            3 -> { customDelegate.invalidate(); custom}
            2 -> { chatDelegate.invalidate(); chat}
            1 -> { tabDelegate.invalidate(); tab }
            else -> { aboveHeadDelegate.invalidate(); aboveHead }
        }
    }

    private val masterToggle = SwitchComponent(Levelhead.displayManager.config.enabled).constrain {
        x = 10.pixels(true)
        y = 10.pixels()
    } childOf titleBar

    private val masterLabel = UIText("§7Master Toggle").constrain {
        x = (masterToggle.getWidth() + 5).pixels(true).to(masterToggle) as XConstraint
        y = 11.pixels()
    } childOf titleBar

    private val editing = DropDown(currentPage, listOf("Head", "Tab", "Chat") +
            if (Levelhead.LevelheadPurchaseStates.customLevelhead)
                listOf("Custom")
            else
                emptyList()
    ).constrain {
        x = (masterLabel.getWidth() + 10).pixels(true).to(masterLabel) as XConstraint
        y = 5.pixels()
    } childOf titleBar

    private val credits = UIText("Remaining credits: ${Levelhead.also { refreshRawPurchases() }.rawPurchases["remaining_levelhead_credits"].asInt}").constrain {
        x = CenterConstraint()
        y = 11.pixels()
    } childOf titleBar

    private val aboveHeadDelegate = invalidateableLazy {
        levelheadContainer {
            title = "Above Head"

            preview = AboveHeadPreviewComponent().constrain {
                width = 100.percent
            }.also {
                Levelhead.selfLevelheadTag.run {
                    val firstLayer = Levelhead.displayManager.aboveHead[0]
                    this.header.let {
                        it.chroma = firstLayer.config.headerChroma
                        it.color = firstLayer.config.headerColor
                        it.value = "${firstLayer.config.headerString}: "
                    }
                    this.footer.let {
                        it.chroma = firstLayer.config.footerChroma
                        it.color = firstLayer.config.footerColor
                        it.value = it.value.substringAfterLast('(').removeSuffix(")")
                    }
                }
            }

            Levelhead.displayManager.aboveHead.forEachIndexed { i, display ->
                if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
                val container = UIContainer().constrain {
                    y = SiblingConstraint(2.5f)
                    width = RelativeConstraint()
                    height = ChildBasedRangeConstraint()
                }
                val toggle = SwitchComponent(display.config.enabled).constrain {
                    x = 5.pixels(alignOpposite = true)
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
                container.constrain {
                    y = SiblingConstraint()
                } childOf settings
            }

            UIContainer().constrain {
                y = SiblingConstraint(2.5f)
                width = 100.percent
                height = 20.pixels
            } childOf settings

            val purchase = ButtonComponent("Purchase more layers") {
                attemptPurchase("head")
            }.constrain {
                x = 12.5.pixels(true)
                y = 7.5.pixels(true).to(divider) as YConstraint
                textScale = 2.5.pixels()
            } childOf this

            val offsetSlider = SliderComponent((Levelhead.displayManager.config.offset * 10).toInt(), 0, 5).constrain {
                x = 12.5.pixels(true)
                y = 5.pixels(alignOutside = true) boundTo purchase
            } childOf this
            offsetSlider.childrenOfType<Slider>().first().constrain {
                width = 80.percent boundTo purchase
            }
            offsetSlider.onValueChange {
                Levelhead.displayManager.config.offset = (it as Int) / 10.0
            }

            val offsetLabel = UIText("Offset").constrain {
                x = 12.5.pixels(true)
                y = 2.5.pixels(alignOutside = true) boundTo offsetSlider
                color = VigilancePalette.getBrightText().constraint
            } childOf this
        }
    }

    private val aboveHead by aboveHeadDelegate

    private var customTag: LevelheadTag = Levelhead.selfLevelheadTag.clone()

    private val customDelegate = invalidateableLazy {
        levelheadContainer {
            title = "Custom Levelhead"

            preview = AboveHeadPreviewComponent().constrain {
                width = 100.percent
            }.also {
                Levelhead.displayManager.aboveHead[0].cache[UPlayer.getUUID()] = customTag.clone()
            }

            val customLevelhead = jsonParser.parse(rawWithAgent("https://api.sk1er.club/levelhead/${UPlayer.getUUID().trimmed}")).asJsonObject
            if (customLevelhead["custom"].asBoolean) {
                CustomLevelheadComponent().constrain {
                    width = RelativeConstraint()
                    height = ChildBasedRangeConstraint()
                } childOf settings
            }
        }
    }

    private val custom by customDelegate

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
                        x = 5.pixels(alignOpposite = true)
                        y = CenterConstraint() boundTo titleText
                    } childOf this@levelheadContainer
                    toggle.onValueChange {
                        this.config.enabled = it as Boolean
                        this.update()
                        preview?.update()
                    }
                }
            } else {
                middleDivider.hide()
                val container = UIContainer().constrain {
                    x = CenterConstraint()
                    y = 25.percent
                    width = 50.percent
                    height = ChildBasedRangeConstraint()
                } childOf settings
                val text = UIText("Levelhead Chat Display not purchased!").constrain {
                    x = CenterConstraint()
                } childOf container
                val purchase = ButtonComponent("Purchase Chat Display") {
                    attemptPurchase("chat")
                }.constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint(5f)
                } childOf container

                purchase.removeEffect<ExpandingClickEffect>()
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
                        x = 5.pixels(alignOpposite = true)
                        y = CenterConstraint() boundTo titleText
                    } childOf this@levelheadContainer
                    toggle.onValueChange {
                        this.config.enabled = it as Boolean
                        this.update()
                        preview?.update()
                    }
                }
            } else {
                middleDivider.hide()
                val container = UIContainer().constrain {
                    x = CenterConstraint()
                    y = 25.percent
                    width = 50.percent
                    height = ChildBasedRangeConstraint()
                } childOf settings
                val text = UIText("Levelhead Tab Display not purchased!").constrain {
                    x = CenterConstraint()
                } childOf container
                val purchase = ButtonComponent("Purchase Tab Display") {
                    attemptPurchase("tab")
                }.constrain {
                    x = CenterConstraint()
                    y = SiblingConstraint(5f)
                } childOf container

                purchase.removeEffect<ExpandingClickEffect>()
            }
        }
    }

    private var tab by tabDelegate

    private var container: LevelheadContainer = when (currentPage) {
        3 -> custom
        2 -> chat
        1 -> tab
        else -> aboveHead
    } childOf content
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
            currentPage = it
            container = when (it) {
                3 -> {customDelegate.invalidate(); custom}
                2 -> chat
                1 -> tab
                else -> {aboveHeadDelegate.invalidate(); aboveHead}
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

        val middleDivider by UIBlock(VigilancePalette.getDivider()).constrain {
            x = 50.percent - 0.5.pixels
            y = 5.pixels
            width = 1.pixel
            height = FillConstraint(useSiblings = false)
        } childOf settingsContainer

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

            GradientComponent(
                VigilancePalette.getBackground().withAlpha(0), VigilancePalette.getBackground(),
                GradientComponent.GradientDirection.TOP_TO_BOTTOM
            ).constrain {
                y = 0.pixels(alignOpposite = true)
                width = 100.percent
                height = 50.pixels
            }.onLeftClick {
                it.stopPropagation()
                scrollBar.mouseClick(it.absoluteX.toDouble(), it.absoluteY.toDouble(), it.mouseButton)
                settings.mouseClick(it.absoluteX.toDouble(), it.absoluteY.toDouble(), it.mouseButton)
            }.onMouseScroll {
                it.stopPropagation()
                settings.mouseScroll(it.delta)
            } childOf settingsContainer

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
                        text = "Could not find the ${type} package."
                        secondaryText = "Please contact Sk1er immediately."
                        confirmButtonText = "Close"
                        denyButtonText = ""
                    } childOf window

                    return
                }
            }

            val remainingCredits = Levelhead.rawPurchases["remaining_levelhead_credits"].asInt
            val cost = seed["cost"].asInt
            when {
                remainingCredits < cost -> {
                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                        text = "Insufficient credits! ${seed["name"].asString} costs $cost credits " +
                                "but you only have ${remainingCredits}."
                        confirmButtonText = "Purchase more credits"
                        onConfirm = {
                            if (!UDesktop.browse(URI.create("https://purchase.sk1er.club/category/1050972"))) {
                                setClipboardString("https://purchase.sk1er.club/category/1050972")
                                EssentialAPI.getNotifications().push("Copied to clipboard!", "Opening browser failed so the link was copied to your clipboard.");
                            }
                        }
                        denyButtonText = "Close"
                    } childOf window
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
                        text = "You are about to purchase the ${name.lowercase()} package." +
                                "\nThis will cost $cost credits. You will have ${remainingCredits - cost} credits left."
                        onConfirm = {
                            Multithreading.submit {
                                val jsonObject =
                                    jsonParser.parse(rawWithAgent("https://api.sk1er.club/levelhead_purchase?access_token=" + Levelhead.auth.accessKey + "&request=" + type + "&hash=" + Levelhead.auth.hash)).asJsonObject
                                if (jsonObject["success"].asBoolean) {
                                    Levelhead.refreshPurchaseStates()
                                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                        text = "Successfully purchased package ${name}."
                                        confirmButtonText = "Close"
                                        onConfirm = {
                                            container = when (editing.getValue()) {
                                                3 -> { customDelegate.invalidate(); custom}
                                                2 -> { chatDelegate.invalidate(); chat}
                                                1 -> { tabDelegate.invalidate(); tab }
                                                else -> { aboveHeadDelegate.invalidate(); aboveHead }
                                            }
                                        }
                                        denyButtonText = ""
                                    } childOf window
                                } else {
                                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                        text = "Failed to purchase package ${name}."
                                        secondaryText = "Cause: ${jsonObject["cause"].asString}"
                                        confirmButtonText = "Close"
                                        denyButtonText = ""
                                        onConfirm = {
                                            container = when (editing.getValue()) {
                                                3 -> { customDelegate.invalidate(); custom}
                                                2 -> { chatDelegate.invalidate(); chat}
                                                1 -> { tabDelegate.invalidate(); tab }
                                                else -> { aboveHeadDelegate.invalidate(); aboveHead }
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
            container = when (editing.getValue()) {
                3 -> { customDelegate.invalidate(); custom}
                2 -> { chatDelegate.invalidate(); chat}
                1 -> { tabDelegate.invalidate(); tab }
                else -> { aboveHeadDelegate.invalidate(); aboveHead }
            }
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
            display.run {
                this.cache.remove(UPlayer.getUUID())
                Levelhead.fetch(UPlayer.getUUID(), this, if (this is AboveHeadDisplay) this.bottomValue else false)
            }.invokeOnCompletion { _ ->
                Levelhead.selfLevelheadTag.run {
                    customTag = this.clone()
                    val firstLayer = Levelhead.displayManager.aboveHead[0]
                    this.header.let {
                        it.chroma = firstLayer.config.headerChroma
                        it.color = firstLayer.config.headerColor
                        it.value = "${firstLayer.config.headerString}: "
                    }
                    this.footer.let {
                        it.chroma = firstLayer.config.footerChroma
                        it.color = firstLayer.config.footerColor
                        it.value = it.value.substringAfterLast('(').removeSuffix(")")
                    }
                }
            }
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
            height = 15.percentOfWindow.coerceAtLeast(25.percent)
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
            this.cache[UPlayer.getUUID()]?.let { tag ->
                tag.header.let { header ->
                    header.chroma = this.config.headerChroma
                    header.color = this.config.headerColor
                    header.value = "${this.config.headerString}: "
                }
                tag.footer.let { footer ->
                    footer.chroma = this.config.footerChroma
                    footer.color = this.config.footerColor
                }
            }
            preview.update()
        }
    }


    private fun levelheadContainer(block: LevelheadContainer.() -> Unit): LevelheadContainer =
        LevelheadContainer().apply(block)
}