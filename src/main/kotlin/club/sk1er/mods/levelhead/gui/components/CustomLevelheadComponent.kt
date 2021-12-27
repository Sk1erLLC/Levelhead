package club.sk1er.mods.levelhead.gui.components

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.trimmed
import club.sk1er.mods.levelhead.core.tryToGetChatColor
import club.sk1er.mods.levelhead.display.LevelheadTag
import club.sk1er.mods.levelhead.gui.LevelheadGUI
import com.google.gson.JsonObject
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.ChatColor
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UScreen
import gg.essential.universal.wrappers.UPlayer
import gg.essential.vigilance.gui.ExpandingClickEffect
import gg.essential.vigilance.gui.VigilancePalette
import gg.essential.vigilance.gui.settings.*
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.awt.Color

class CustomLevelheadComponent: UIComponent() {

    init {
        val newStatusMaybe = getProposalStatus()["status"]?.asString ?: "not purchased"
        if (status.get() != newStatusMaybe) status.set(newStatusMaybe)
    }

    val fakeRequest = JsonObject()

    val resetButton = ButtonComponent("Reset to default") {
        Levelhead.scope.launch {
            clearLevelhead("message")
        }.invokeOnCompletion {
            Window.enqueueRenderOperation {
                Levelhead.displayManager.aboveHead[0].cache.remove(UPlayer.getUUID())
                Levelhead.fetch(listOf(Levelhead.LevelheadRequest(UPlayer.getUUID().trimmed, Levelhead.displayManager.aboveHead[0], Levelhead.displayManager.aboveHead[0].bottomValue)))
                    .invokeOnCompletion {
                        if (!Levelhead.LevelheadPurchaseStates.customLevelhead) {
                            Window.enqueueRenderOperation {
                                this.hide()
                            }
                        } else {
                            Levelhead.selfLevelheadTag.header.run {
                                currentHeader?.setText(this.value)
                                currentHeader?.constraints?.color = if (this.chroma) basicColorConstraint { Levelhead.chromaColor } else this.color.constraint
                            }
                            Levelhead.selfLevelheadTag.footer.run {
                                currentFooter?.setText(this.value)
                                currentFooter?.constraints?.color = if (this.chroma) basicColorConstraint { Levelhead.chromaColor } else this.color.constraint
                            }
                        }
                        currentHeader?.let { header ->
                            header.setText(Levelhead.selfLevelheadTag.header.value)
                            header.constraints.color = if (Levelhead.selfLevelheadTag.header.chroma)
                                basicColorConstraint { Levelhead.chromaColor }
                            else
                                Levelhead.selfLevelheadTag.header.color.constraint
                        }
                        currentFooter?.let { footer ->
                            footer.setText(Levelhead.selfLevelheadTag.footer.value)
                            footer.constraints.color = if (Levelhead.selfLevelheadTag.footer.chroma)
                                basicColorConstraint { Levelhead.chromaColor }
                            else
                                Levelhead.selfLevelheadTag.footer.color.constraint
                        }
                    }
            }
        }
    }.constrain {
        x = 5.pixels(alignOpposite = true) - 50.percent
        y = 0.pixels
    } childOf this
    // get rid of the stupid circle being stuck
    init {
        resetButton.removeEffect<ExpandingClickEffect>()
    }
    val text = UIText("§nCustom Levelhead").constrain {
        x = 0.pixels
        y = CenterConstraint() boundTo resetButton
    } childOf this
    val currentLabel = UIText("Current Text").constrain {
        x = 5.5.pixels + 50.percent
        y = CenterConstraint() boundTo resetButton
    } childOf this
    val currentLevelhead
        get() = Levelhead.displayManager.aboveHead[0].cache[UPlayer.getUUID()]
    val createFooter: (LevelheadTag.LevelheadComponent) -> UIText = { footer ->
        UIText(footer.value).constrain {
            x = 2.5.pixels(true)
            y = CopyConstraintFloat() boundTo currentLabel
            color = if (footer.chroma) basicColorConstraint { Levelhead.chromaColor } else footer.color.constraint
        } childOf this
    }
    var currentFooter = currentLevelhead?.footer?.let(createFooter)
    val createHeader: (LevelheadTag.LevelheadComponent) -> UIText = { header ->
        UIText(header.value).constrain {
            x = SiblingConstraint(2.5f, true)
            y = CopyConstraintFloat() boundTo currentLabel
            color = if (header.chroma) basicColorConstraint { Levelhead.chromaColor } else header.color.constraint
        } childOf this
    }
    var currentHeader = currentLevelhead?.header?.let(createHeader)
    val content = UIContainer().constrain {
        y = SiblingConstraint(5f) boundTo resetButton
        height = ChildBasedRangeConstraint()
        width = RelativeConstraint()
    } childOf this
    init {
        if (Levelhead.LevelheadPurchaseStates.customLevelhead) {
            content.createComponents()
        }
    }

    private fun UIComponent.createComponents() {
        val statusLabel = UIText("Proposal Status").constrain {
            x = 2.5.pixels
            y = 0.pixels
        } childOf this
        val statusText = UIText().constrain {
            x = 5.pixels(true) - 50.percent
            y = 0.pixels
        } childOf this
        statusText.bindText(capitallizedStatus)

        val currentProposalLabel = UIText("Current Proposal:").constrain {
            x = 5.5.pixels + 50.percent
            y = 0.pixels
        } childOf this

        val currentProposal = getProposalInfo()
        val request = currentProposal["request"]?.asJsonObject
        var (proposalHeader, proposalFooter) = parseProposal(
            this,
            currentProposalLabel,
            request
        ).also {
            it.first.constraints.color = if (it.first.getText() == "No current proposal") Color.WHITE.constraint else
                (request?.get("header_obj")?.asJsonObject ?:
                currentProposal["current"]?.asJsonObject?.get("header_obj")?.asJsonObject)?.let { header ->
                    if (header["chroma"].asBoolean) basicColorConstraint { Levelhead.chromaColor } else
                        Color(header["red"].asInt, header["green"].asInt, header["blue"].asInt).constraint
                } ?: Levelhead.displayManager.aboveHead[0].config.headerColor.constraint
            it.second.constraints.color = if (it.second.getText().isEmpty()) Color.WHITE.constraint else
                (request?.get("footer_obj")?.asJsonObject ?:
                currentProposal["current"]?.asJsonObject?.get("footer_obj")?.asJsonObject)?.let { footer ->
                    if (footer["chroma"].asBoolean) basicColorConstraint { Levelhead.chromaColor } else
                        Color(footer["red"].asInt, footer["green"].asInt, footer["blue"].asInt).constraint
                } ?: Levelhead.displayManager.aboveHead[0].config.footerColor.constraint
        }

        var headerText = ""
        val headerInput = TextComponent("", "Header", false, false).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(5f)
        } childOf this effect OutlineEffect(VigilancePalette.getOutline(), 1f)
        headerInput.onValueChange {
            if (it !is String || it.length > 15) return@onValueChange
            headerText = it
        }
        var footerText = ""
        val footerInput = TextComponent("", "Footer", false, false).constrain {
            x = SiblingConstraint(5f)
            y = CopyConstraintFloat() boundTo headerInput
        } childOf this effect OutlineEffect(VigilancePalette.getOutline(), 1f)
        footerInput.onValueChange {
            if (it !is String || it.length > 15) return@onValueChange
            footerText = it
        }
        val proposeButton = ButtonComponent("Propose") {
            Levelhead.scope.launch {
                proposeLevelhead(headerText, footerText)
            }.invokeOnCompletion {
                fakeRequest.addProperty("strlevel", footerText)
                fakeRequest.addProperty("header", headerText)
                Window.enqueueRenderOperation {
                    parseProposal(this, currentProposalLabel, fakeRequest).also {
                        proposalHeader.hide()
                        proposalHeader = it.first
                        proposalFooter.hide()
                        proposalFooter = it.second
                        currentProposal["current"]?.asJsonObject?.run {
                            this["header_obj"]?.asJsonObject?.let { obj ->
                                proposalHeader.constraints.color = if (obj["chroma"].asBoolean)
                                    basicColorConstraint { Levelhead.chromaColor }
                                else
                                    Color(obj["red"].asInt, obj["green"].asInt, obj["blue"].asInt).constraint
                            }
                            this["footer_obj"]?.asJsonObject?.let { obj ->
                                proposalFooter.constraints.color = if (obj["chroma"].asBoolean)
                                    basicColorConstraint { Levelhead.chromaColor }
                                else
                                    Color(obj["red"].asInt, obj["green"].asInt, obj["blue"].asInt).constraint
                            }
                        }
                    }
                }
                val newStatusMaybe = getProposalStatus()["status"]?.asString ?: "not purchased"
                if (status.get() != newStatusMaybe) status.set(newStatusMaybe)
            }
        }.constrain {
            x = 5.pixels(true) - 50.percent
            y = (CopyConstraintFloat() boundTo headerInput) - 2.5.pixels
        } childOf this

        val clearProposalButton = ButtonComponent("Clear Proposal") {
            Levelhead.scope.launch {
                clearLevelhead("proposal")
            }.invokeOnCompletion {
                Window.enqueueRenderOperation {
                    parseProposal(this, currentProposalLabel, null).also {
                        proposalHeader.hide()
                        proposalHeader = it.first
                        proposalFooter.hide()
                        proposalFooter = it.second
                    }
                }
                fakeRequest.remove("header")
                fakeRequest.remove("strlevel")
                val newStatusMaybe = getProposalStatus()["status"]?.asString ?: "not purchased"
                if (status.get() != newStatusMaybe) status.set(newStatusMaybe)
            }
        }.constrain {
            x = 5.5.pixels + 50.percent
            y = CopyConstraintFloat() boundTo proposeButton
        } childOf this

        val headerColorComponent = (currentProposal["current"].asJsonObject["header_obj"]?.asJsonObject?.let { headerObj ->
            CustomColorSetting(true, false, headerObj["chroma"].asBoolean, Color(
                headerObj["red"].asInt,
                headerObj["green"].asInt,
                headerObj["blue"].asInt
            ))
        } ?: CustomColorSetting(true, true, Levelhead.selfLevelheadTag.header.chroma, Levelhead.selfLevelheadTag.header.color)
                ).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(2.5f)
            width = RelativeConstraint(0.5f) - 8.pixels()
            height = AspectConstraint(0.4f)
        } childOf this
        headerColorComponent.onValueChange {
            if (Levelhead.selfLevelheadTag.header.chroma) {
                currentHeader?.constraints?.color = basicColorConstraint { Levelhead.chromaColor }
            } else {
                Levelhead.selfLevelheadTag.header.color = it
                currentHeader?.constraints?.color = it.constraint
            }
        }
        val footerColorComponent = (currentProposal["current"].asJsonObject["footer_obj"]?.asJsonObject?.let { footerObj ->
            CustomColorSetting(false, false, footerObj["chroma"].asBoolean, Color(
                footerObj["red"].asInt,
                footerObj["green"].asInt,
                footerObj["blue"].asInt
            ))
        } ?: CustomColorSetting(false, true, Levelhead.selfLevelheadTag.footer.chroma, Levelhead.selfLevelheadTag.footer.color)
                ).constrain {
            x = 5.5.pixels + 50.percent
            y = CopyConstraintFloat() boundTo headerColorComponent
            width = RelativeConstraint(0.5f) - 9.pixels()
            height = AspectConstraint(0.4f)
        } childOf this
        footerColorComponent.onValueChange {
            if (Levelhead.selfLevelheadTag.footer.chroma) {
                currentFooter?.constraints?.color = basicColorConstraint { Levelhead.chromaColor }
            } else {
                Levelhead.selfLevelheadTag.footer.color = it
                currentFooter?.constraints?.color = it.constraint
            }
        }
        Window.enqueueRenderOperation {
            (UScreen.currentScreen!! as LevelheadGUI).onScreenClose {
                Levelhead.scope.launch {
                    setLevelheadColor(
                        headerColorComponent.dropdown.getValue() == 0,
                        headerColorComponent.selector.getCurrentColor().withAlpha(
                            if (headerColorComponent.dropdown.getValue() == 2) 0f else 1f
                                ),
                        footerColorComponent.dropdown.getValue() == 0,
                        footerColorComponent.selector.getCurrentColor().withAlpha(
                            if (footerColorComponent.dropdown.getValue() == 2) 0f else 1f
                        )
                    )
                }
            }
        }

        val clearColorButton = ButtonComponent("Clear Colors") {
            Levelhead.scope.launch {
                clearLevelheadColor()
            }.invokeOnCompletion {
                Window.enqueueRenderOperation {
                    parseProposal(this, currentProposalLabel, fakeRequest).also {
                        proposalHeader.hide()
                        proposalHeader = it.first
                        if (fakeRequest.has("header")) {
                            proposalHeader.constraints.color = Levelhead.selfLevelheadTag.header.color.constraint
                        }
                        proposalFooter.hide()
                        proposalFooter = it.second
                        if (fakeRequest.has("strlevel")) {
                            proposalFooter.constraints.color = Levelhead.selfLevelheadTag.footer.color.constraint
                        }
                    }
                    headerColorComponent.dropdown.select(2)
                    footerColorComponent.dropdown.select(2)
                    Levelhead.displayManager.aboveHead[0].run {
                        this.cache[UPlayer.getUUID()]?.let { tag ->
                            tag.header.let { header ->
                                header.chroma = this.config.headerChroma
                                header.color = this.config.headerColor
                            }
                            tag.footer.let { footer ->
                                footer.chroma = this.config.footerChroma
                                footer.color = this.config.footerColor
                            }
                        }
                    }
                    delay(100) {
                        this@CustomLevelheadComponent.currentHeader?.constraints?.color =
                            if (Levelhead.displayManager.aboveHead[0].config.headerChroma)
                                basicColorConstraint { Levelhead.chromaColor }
                            else
                                Levelhead.displayManager.aboveHead[0].config.headerColor.constraint
                        this@CustomLevelheadComponent.currentFooter?.constraints?.color =
                            if (Levelhead.displayManager.aboveHead[0].config.footerChroma)
                                basicColorConstraint { Levelhead.chromaColor }
                            else
                                Levelhead.displayManager.aboveHead[0].config.footerColor.constraint
                    }
                }
            }
        }.constrain {
            x = 2.5.pixels(true)
            y = CopyConstraintFloat() boundTo clearProposalButton
        } childOf this
    }

    private class CustomColorSetting(header: Boolean, none: Boolean, initialChroma: Boolean, initialColor: Color): UIComponent() {
        var valueChangecallback: (Color) -> Unit = {}

        fun onValueChange(listener: (Color) -> Unit): CustomColorSetting {
            valueChangecallback = listener
            return this
        }

        val display = Levelhead.displayManager.aboveHead[0]
        val colorLabel = UIText(if (header) "Header Color:" else "Footer Color:").constrain {
            x = 0.pixels
            y = 4.5.pixels
        } childOf this
        val options = listOf("Chroma", "RGB", "None") + ChatColor.values().filter { it.isColor() }
        val dropdown: DropDown = DropDown(
            if (initialChroma) 0 else if (none) 2 else initialColor.tryToGetChatColor()?.let { options.indexOf(it ) } ?: 1,
            options.map {
                if (it is ChatColor)
                    "${it}${it.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}"
                else it.toString()
            }
        ).constrain {
            x = 0.pixels(true)
            y = 0.pixels
        }.childOf(this).also {
            UScreen.currentScreen?.let { screen ->
                if (screen !is LevelheadGUI) return@let
                Window.enqueueRenderOperation { screen.dropdowns.add(it) }
            }
            it.onValueChange {
                when (it) {
                    0 -> if (header) {
                        Levelhead.selfLevelheadTag.header.chroma = true
                        valueChangecallback(selector.getCurrentColor())
                    } else {
                        Levelhead.selfLevelheadTag.footer.chroma = true
                        valueChangecallback(selector.getCurrentColor())
                    }
                    1 -> if (header) {
                        Levelhead.selfLevelheadTag.header.chroma = false
                        valueChangecallback(selector.getCurrentColor())
                    } else {
                        Levelhead.selfLevelheadTag.footer.chroma = false
                        valueChangecallback(selector.getCurrentColor())
                    }
                    2 -> if (header) {
                        valueChangecallback(display.config.headerColor)
                    } else {
                        valueChangecallback(display.config.footerColor)
                    }
                    else -> {
                        if (header) {
                            Levelhead.selfLevelheadTag.header.chroma = true
                        } else {
                            Levelhead.selfLevelheadTag.footer.chroma = true
                        }
                        valueChangecallback((options[it] as ChatColor).color!!)
                        if (selector.getCurrentColor() == (options[it] as ChatColor).color!!) return@onValueChange
                        val (red, green, blue) = (options[it] as ChatColor).color!!
                        val (h,s,b) = Color.RGBtoHSB(red, green, blue, null)
                        selector.setHSB(h, s, b)
                    }
                }
            }
        }
        val selector: ColorPicker = ColorPicker(
            if (initialChroma || none) {
                if (header)
                    display.config.headerColor
                else
                    display.config.footerColor
            } else initialColor,
            false
        ).constrain {
            x = CenterConstraint()
            y = SiblingConstraint(10f).to(colorLabel) as YConstraint
            width = AspectConstraint(1.25f)
            height = 15.percentOfWindow.coerceAtLeast(25.percent)
        }.childOf(this).apply {
            this.onValueChange {
                if (dropdown.getValue() == 0) return@onValueChange
                dropdown.select(1)
            }
        }

        override fun draw(matrixStack: UMatrixStack) {
            super.draw(matrixStack)
            if (dropdown.getValue() == 0) {
                val color = Levelhead.chromaColor
                val (hue, saturation, brightness) = Color.RGBtoHSB(color.red, color.green, color.blue, null)
                selector.setHSB(hue, saturation, brightness)
            }
        }

    }

    private fun parseProposal(uiComponent: UIComponent, label: UIText, request: JsonObject?): Pair<UIText, UIText> {
        val aboveHeadDisplayConfig = Levelhead.displayManager.aboveHead[0].config
        val proposalFooter = UIText(
            request?.get("strlevel")?.asString?.replace(
                    defaultRegex,
                    ""
                )?.let { "$it (${Levelhead.selfLevelheadTag.footer.value.substringAfterLast('(').removeSuffix(")")})" }
                ?: ""
        ).constrain {
            x = 2.5.pixels(true)
            y = CopyConstraintFloat() boundTo label
        } childOf uiComponent
        val proposalHeader = UIText((request?.get("header")?.asString?.replace(defaultRegex, Levelhead.selfLevelheadTag.header.value)?.let { "$it: " }) ?: "No current proposal").constrain {
            x = SiblingConstraint(2.5f, true) boundTo proposalFooter
            y = CopyConstraintFloat() boundTo label
        } childOf uiComponent
        return Pair(proposalHeader, proposalFooter)

    }


    companion object {

        var status: State<String> = BasicState(getProposalStatus()["status"]?.asString ?: "not purchased")
        val capitallizedStatus = status.map {
            it.split(' ').joinToString(separator = " ") { word -> word.replaceFirstChar { char -> char.titlecase() } }
        }
        val defaultRegex = Regex("\\Adefault\\Z", RegexOption.IGNORE_CASE)

        private fun proposeLevelhead(header: String, footer: String) {
            val request = Request.Builder()
                .url("https://api.sk1er.club/levelheadapi/propose?auth=${Levelhead.auth.hash}&header=$header&level=$footer")
                .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${Levelhead.VERSION})")
                .post(Levelhead.EMPTY_BODY)
                .build()
            Levelhead.okHttpClient.newCall(request).execute().close()
        }

        private fun clearLevelhead(type: String) {
            val request = Request.Builder()
                .url("https://api.sk1er.club/levelheadapi/clear?auth=${Levelhead.auth.hash}&type=$type")
                .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${Levelhead.VERSION})")
                .post(Levelhead.EMPTY_BODY)
                .build()
            Levelhead.okHttpClient.newCall(request).execute().close()
        }

        private fun clearLevelheadColor() {
            val request = Request.Builder()
                .url("https://api.sk1er.club/levelheadapi/clearcolor?auth=${Levelhead.auth.hash}")
                .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${Levelhead.VERSION})")
                .post(Levelhead.EMPTY_BODY)
                .build()
            Levelhead.okHttpClient.newCall(request).execute().close()
        }

        private fun getProposalStatus(): JsonObject {
            val request = Request.Builder()
                .url("https://api.sk1er.club/levelheadapi/status?auth=${Levelhead.auth.hash}")
                .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${Levelhead.VERSION})")
                .post(Levelhead.EMPTY_BODY)
                .build()
            val data = kotlin.runCatching {
                Levelhead.okHttpClient.newCall(request).execute().body()?.use { it.string() }!!
            }.getOrDefault("{\"success\":false,\"cause\":\"API_DOWN\"}")
            return Levelhead.jsonParser.parse(data).asJsonObject
        }

        private fun getProposalInfo(): JsonObject =
            Levelhead.jsonParser.parse(Levelhead.getWithAgent("https://api.sk1er.club/levelheadapi/info?auth=${Levelhead.auth.hash}")).asJsonObject

        private fun setLevelheadColor(headerChroma: Boolean, headerColor: Color, footerChroma: Boolean, footerColor: Color) {
            val colors = JsonObject()
            colors.addProperty("headerChroma", headerChroma)
            colors.addProperty("headerColor", headerColor.rgb)
            colors.addProperty("footerChroma", footerChroma)
            colors.addProperty("footerColor", footerColor.rgb)
            val body = RequestBody.create(MediaType.parse("application/json"), Levelhead.gson.toJson(colors))
            val request = Request.Builder()
                .url("https://api.sk1er.club/levelheadapi/color?auth=${Levelhead.auth.hash}")
                .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${Levelhead.VERSION})")
                .header("content-type", "application/json")
                .post(body)
                .build()
            Levelhead.okHttpClient.newCall(request).execute().close()
        }
    }
}