package club.sk1er.mods.levelhead.gui

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.Levelhead.getWithAgent
import club.sk1er.mods.levelhead.Levelhead.refreshRawPurchases
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.*
import club.sk1er.mods.levelhead.display.*
import club.sk1er.mods.levelhead.gui.components.SliderComponent
import com.google.gson.JsonObject
import gg.essential.api.EssentialAPI
import gg.essential.api.gui.EssentialGUI
import gg.essential.api.gui.buildConfirmationModal
import gg.essential.api.utils.Multithreading
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.AbstractTextInput
import gg.essential.elementa.components.inspector.Inspector
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.ChatColor
import gg.essential.universal.UDesktop
import gg.essential.universal.UMatrixStack
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.ExpandingClickEffect
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import gg.essential.vigilance.utils.onLeftClick
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.awt.Color
import java.net.URI
import kotlin.properties.Delegates

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
        newGuiScale = EssentialAPI.getGuiUtil().getGuiScale(855)
        shouldClearDropdowns = false
        container = when (editing.getValue()) {
            3 -> { customDelegate.invalidate(); custom}
            2 -> { chatDelegate.invalidate(); chat}
            1 -> { tabDelegate.invalidate(); tab }
            else -> { aboveHeadDelegate.invalidate(); aboveHead }
        }
    }

    val dropdowns = mutableListOf<DropDown>()

    private val masterToggle = SwitchComponent(Levelhead.displayManager.config.enabled).constrain {
        x = 10.pixels(true)
        y = 10.pixels()
    } childOf titleBar

    private val masterLabel = UIText("§7Mod Toggle").constrain {
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

    private val credits = UIText("Credits: ${Levelhead.also { it.scope.launch { refreshRawPurchases() } }.rawPurchases["remaining_levelhead_credits"]?.asInt ?: 0}").constrain {
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

            Window.enqueueRenderOperation {
                Levelhead.displayManager.aboveHead.forEachIndexed { i, display ->
                    if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
                    val container = UIContainer().constrain {
                        y = SiblingConstraint(2.5f)
                        width = RelativeConstraint()
                        height = ChildBasedRangeConstraint()
                    }
                    val text = UIText("§nLayer ${i + 1}").constrain {
                        x = 0.pixels
                        y = 0.pixels
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
                    height = 20.percentOfWindow
                } childOf settings
            }

            var purchaseBool = false
            if (Levelhead.LevelheadPurchaseStates.aboveHead < 3) {
                val purchase = ButtonComponent("Purchase more layers") {
                    attemptPurchase("head")
                }.constrain {
                    x = 12.5.pixels(true)
                    y = 7.5.pixels(true) boundTo divider
                } childOf this

                // I HATE YOU STUCK CIRCLE!!!!!!!!!!
                purchase.removeEffect<ExpandingClickEffect>()

                purchaseBool = true
            }

            val offsetSlider = SliderComponent((Levelhead.displayManager.config.offset * 10).toInt(), 0, 5).constrain {
                x = 12.5.pixels(true)
                y = if (purchaseBool)
                    SiblingConstraint(2.5f, true)
                else
                    2.5.pixels(true) boundTo divider
                width = 120.pixels
            } childOf this
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

            Window.enqueueRenderOperation {
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
                "${UPlayer.getPlayer()?.displayName?.formattedText?.let { if (it.dropLast(2).endsWith(']')) it.substringBeforeLast(' ') else it } ?: "Sk1er"}§r: Hi!"
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

    var shouldClearDropdowns = false

    private var container: LevelheadContainer = when (currentPage) {
        3 -> custom
        2 -> chat
        1 -> tab
        else -> aboveHead
    } childOf content
        set(value) {
            Window.enqueueRenderOperation {
                if (shouldClearDropdowns) {
                    dropdowns.forEach {
                        if (!it.hasParent || Window.ofOrNull(it) == null) return@forEach
                        it.collapse(true, true)
                        it.hide()
                    }
                    dropdowns.clear()
                }
                value.childOf(content)
                container.hide()
                value.unhide()
                field = value
            }
        }

    init {
        masterToggle.onValueChange {
            Levelhead.displayManager.config.enabled = it as Boolean
        }

        editing.onValueChange {
            currentPage = it
            shouldClearDropdowns = true
            container = when (it) {
                3 -> {customDelegate.invalidate(); custom}
                2 -> {chatDelegate.invalidate(); chat}
                1 -> {tabDelegate.invalidate(); tab}
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

        val titleText by UIText(shadow = false).constrain {
            x = 10.pixels()
            y = 7.5.pixels(true) boundTo divider
            textScale = 2.pixels
        } childOf this

        private val scrollContainer = UIContainer().constrain {
            x = 5.pixels
            y = 5.pixels
            width = RelativeConstraint()
            height = FillConstraint(false)
        } childOf settingsContainer

        val scrollBar = UIBlock(VigilancePalette.getScrollBar()).constrain {
            x = 98.6.percent
            width = 1.percent
        }

        val settings = ScrollComponent().constrain {
            width = 98.percent
            height = 100.percent
        } childOf scrollContainer

        val middleDivider by UIBlock(VigilancePalette.getDivider()).constrain {
            x = CenterConstraint() boundTo settings
            y = 5.pixels
            width = 1.pixel
            height = FillConstraint(useSiblings = false)
        } childOf settingsContainer

        init {
            if (editing.getValue() == 0) {
                settings.setVerticalScrollBarComponent(scrollBar)
                scrollBar childOf settingsContainer
            }

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
            }.onLeftClick { event ->
                event.stopPropagation()
                scrollBar.takeIf { it.hasParent }?.mouseClick(event.absoluteX.toDouble(), event.absoluteY.toDouble(), event.mouseButton)
                settings.takeIf { it.hasParent }?.mouseClick(event.absoluteX.toDouble(), event.absoluteY.toDouble(), event.mouseButton)
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
                                    jsonParser.parse(getWithAgent("https://api.sk1er.club/levelhead_purchase?access_token=" + Levelhead.auth.accessKey + "&request=" + type + "&hash=" + Levelhead.auth.hash)).asJsonObject
                                if (jsonObject["success"].asBoolean) {
                                    Levelhead.scope.launch {
                                        Levelhead.refreshPurchaseStates()
                                    }
                                    EssentialAPI.getEssentialComponentFactory().buildConfirmationModal {
                                        text = "Successfully purchased package ${name}."
                                        confirmButtonText = "Close"
                                        onConfirm = {
                                            shouldClearDropdowns = false
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
                                            shouldClearDropdowns = false
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
            Levelhead.scope.launch {
                refreshRawPurchases()
            }.invokeOnCompletion {
                credits.setText("Remaining credits: ${Levelhead.rawPurchases["remaining_levelhead_credits"].asInt}")
                shouldClearDropdowns = false
                container = when (editing.getValue()) {
                    3 -> { customDelegate.invalidate(); custom}
                    2 -> { chatDelegate.invalidate(); chat}
                    1 -> { tabDelegate.invalidate(); tab }
                    else -> { aboveHeadDelegate.invalidate(); aboveHead }
                }
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
        val options = Levelhead.allowedTypes
        val type = DropDown(
            options.entrySet().map { it.key }.sortedBy { string -> string }.indexOf(display.config.type).coerceAtLeast(0),
            options.entrySet().sortedBy { it.key }.map { it.value.asJsonObject["name"].asString }
        ).constrain {
            x = 5.pixels(true)
            y = CramSiblingConstraint() + 7.pixels
        } childOf rightContainer
        val typeLabel = UIText("Type: ").constrain {
            x = 5.pixels()
            y = (CopyConstraintFloat() boundTo type) + 5.5.pixels
        } childOf rightContainer
        Window.enqueueRenderOperation {
            dropdowns.add(type)
        }
        type.onValueChange {
            display.config.type = options.entrySet().map { it.key }.sortedBy { string -> string }[it]
            display.run {
                this.cache.remove(UPlayer.getUUID())
                Levelhead.fetch(listOf(Levelhead.LevelheadRequest(UPlayer.getUUID().trimmed, this, if (this is AboveHeadDisplay) this.bottomValue else false)))
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
        val showToggle = SwitchComponent(display.config.showSelf).constrain {
            x = 5.75.pixels(true)
            y = CenterConstraint() boundTo typeLabel
        } childOf leftContainer
        val showLabel = UIText("Show on self").constrain {
            x = 2.5.pixels()
            y = CenterConstraint() boundTo showToggle
        } childOf leftContainer
        showToggle.onValueChange {
            display.config.showSelf = it as Boolean
            if (display !is AboveHeadDisplay)
                preview.update()
        }
        if (display is AboveHeadDisplay) {
            val toggle = SwitchComponent(display.config.enabled).constrain {
                x = 5.pixels(alignOpposite = true)
                y = 0.pixels
            }
            rightContainer.insertChildBefore(toggle, type)
            toggle.onValueChange {
                display.config.enabled = it as Boolean
            }
            val toggleLabel = UIText("Layer Toggle").constrain {
                x = 5.pixels
                y = CenterConstraint() boundTo toggle
            }
            rightContainer.insertChildBefore(toggleLabel, toggle)
            val textLabel = UIText("Prefix: ").constrain {
                x = 2.5.pixels()
                y = CenterConstraint() boundTo typeLabel
            } childOf leftContainer
            val textInput = TextComponent(display.config.headerString, "", false, false).constrain {
                x = 6.pixels(true)
                y = CenterConstraint() boundTo typeLabel
                height = type.constraints.height
            } childOf leftContainer
            textInput.childrenOfType<UIBlock>().first()
                .also { it.childrenOfType<AbstractTextInput>().first().constraints.y = CenterConstraint() }
                .constraints.height = 100.percent
            showToggle.constraints.y = CramSiblingConstraint(7f)
            textInput.onValueChange {
                if (it !is String) return@onValueChange
                display.config.headerString = it
                display.update()
            }
        }
        val header = ColorSetting(true, display, preview).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(7f)
            width = RelativeConstraint() - 8.5.pixels()
            height = AspectConstraint(0.4f)
        } childOf leftContainer
        if (display !is TabDisplay)
            ColorSetting(false, display, preview).constrain {
                x = CenterConstraint()
                y = if (display is ChatDisplay)
                    (CopyConstraintFloat() boundTo header) + 3.pixels
                else
                    CopyConstraintFloat() boundTo header
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
            currentScreen?.let { screen ->
                if (screen !is LevelheadGUI) return@let
                Window.enqueueRenderOperation { screen.dropdowns.add(color) }
            }
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
                if (color.getValue() == 0) return@onValueChange
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
            it.onMouseClick {
                color.select(1)
            }
        }
            set(value) {
                if (display is ChatDisplay) return
                field = value
            }

        init {
            if (display is ChatDisplay) selector.hide()
        }

        override fun draw(matrixStack: UMatrixStack) {
            super.draw(matrixStack)
            if (color.getValue() == 0) {
                val color = Levelhead.chromaColor
                val (hue, saturation, brightness) = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                selector.setHSB(hue, saturation, brightness)
            }
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