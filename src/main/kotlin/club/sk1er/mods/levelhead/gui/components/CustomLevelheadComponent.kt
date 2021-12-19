package club.sk1er.mods.levelhead.gui.components

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.tryToGetChatColor
import club.sk1er.mods.levelhead.core.update
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import club.sk1er.mods.levelhead.display.LevelheadTag
import com.google.gson.JsonObject
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
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
                Levelhead.displayManager.aboveHead[0].update()
                if (!Levelhead.LevelheadPurchaseStates.customLevelhead) this.hide()
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
    val divider = UIBlock(VigilancePalette.getDivider()).constrain {
        x = 50.percent - 0.5.pixels
        y = 0.pixels
        width = 1.pixel
        height = CopyConstraintFloat() boundTo resetButton
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
            color = if (footer.chroma) basicColorConstraint { Color(Levelhead.ChromaColor) } else footer.color.constraint
        } childOf this
    }
    var currentFooter = currentLevelhead?.footer?.let(createFooter)
    val createHeader: (LevelheadTag.LevelheadComponent) -> UIText = { header ->
        UIText(header.value).constrain {
            x = SiblingConstraint(2.5f, true)
            y = CopyConstraintFloat() boundTo currentLabel
            color = if (header.chroma) basicColorConstraint { Color(Levelhead.ChromaColor) } else header.color.constraint
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
        divider.constraints.height += 200.pixels
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
        var (proposalHeader, proposalFooter) = parseProposal(
            this,
            currentProposalLabel,
            currentProposal["request"]?.asJsonObject
        ).also {
            it.first.constraints.color = currentLevelhead?.header?.let { header ->
                if (header.chroma) basicColorConstraint { Color(Levelhead.ChromaColor) } else header.color.constraint
            } ?: Color.WHITE.constraint
            it.second.constraints.color = currentLevelhead?.footer?.let { footer ->
                if (footer.chroma) basicColorConstraint { Color(Levelhead.ChromaColor) } else footer.color.constraint
            } ?: Color.WHITE.constraint
        }

        var headerText = ""
        val headerInput = TextComponent("", "Header", false, false).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(5f)
        } childOf this effect OutlineEffect(VigilancePalette.getOutline(), 1f)
        headerInput.onValueChange {
            headerText = it as String
        }
        var footerText = ""
        val footerInput = TextComponent("", "Footer", false, false).constrain {
            x = SiblingConstraint(5f)
            y = CopyConstraintFloat() boundTo headerInput
        } childOf this effect OutlineEffect(VigilancePalette.getOutline(), 1f)
        footerInput.onValueChange {
            footerText = it as String
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
                                    basicColorConstraint { Color(Levelhead.ChromaColor) }
                                else
                                    Color(obj["red"].asInt, obj["green"].asInt, obj["blue"].asInt).constraint
                            }
                            this["footer_obj"]?.asJsonObject?.let { obj ->
                                proposalFooter.constraints.color = if (obj["chroma"].asBoolean)
                                    basicColorConstraint { Color(Levelhead.ChromaColor) }
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

        val headerColorComponent = CustomColorSetting(true).constrain {
            x = 2.5.pixels
            y = SiblingConstraint(2.5f)
            width = RelativeConstraint(0.5f) - 7.5.pixels()
            height = AspectConstraint(0.4f)
        } childOf this
        val footerColorComponent = CustomColorSetting(false).constrain {
            x = 5.5.pixels + 50.percent
            y = CopyConstraintFloat() boundTo headerColorComponent
            width = RelativeConstraint(0.5f) - 7.5.pixels()
            height = AspectConstraint(0.4f)
        } childOf this

        val sendColorButton = ButtonComponent("Send Colors") {
            Levelhead.scope.launch {
                setLevelheadColor(
                    headerColorComponent.dropdown.getValue() == 0,
                    headerColorComponent.selector.getCurrentColor(),
                    footerColorComponent.dropdown.getValue() == 0,
                    footerColorComponent.selector.getCurrentColor()
                )
            }.invokeOnCompletion {
                Window.enqueueRenderOperation {
                    parseProposal(this, currentProposalLabel, fakeRequest).also {
                        proposalHeader.hide()
                        proposalHeader = it.first
                        if (fakeRequest.has("header")) {
                            proposalHeader.constraints.color =
                                if (headerColorComponent.dropdown.getValue() == 0)
                                    basicColorConstraint { Color(Levelhead.ChromaColor) }
                                else headerColorComponent.selector.getCurrentColor().constraint
                        }
                        proposalFooter.hide()
                        proposalFooter = it.second
                        if (fakeRequest.has("strlevel")) {
                            proposalFooter.constraints.color =
                                if (footerColorComponent.dropdown.getValue() == 0)
                                    basicColorConstraint { Color(Levelhead.ChromaColor) }
                                else footerColorComponent.selector.getCurrentColor().constraint
                        }
                    }
                    Levelhead.displayManager.aboveHead[0].update()
                    this@CustomLevelheadComponent.currentHeader?.setColor(
                        if (headerColorComponent.dropdown.getValue() == 0)
                            basicColorConstraint { Color(Levelhead.ChromaColor) }
                        else headerColorComponent.selector.getCurrentColor().constraint
                    )
                    this@CustomLevelheadComponent.currentFooter?.setColor(
                        if (footerColorComponent.dropdown.getValue() == 0)
                            basicColorConstraint { Color(Levelhead.ChromaColor) }
                        else footerColorComponent.selector.getCurrentColor().constraint
                    )
                }
            }
        }.constrain {
            x = 2.5.pixels(true)
            y = CopyConstraintFloat() boundTo clearProposalButton
        } childOf this
    }

    private class CustomColorSetting(header: Boolean): UIComponent() {
        val display = Levelhead.displayManager.aboveHead[0]
        val colorLabel = UIText(if (header) "Header Color:" else "Footer Color:").constrain {
            x = 0.pixels
            y = 4.5.pixels
        } childOf this
        val options = listOf("Chroma", "RGB") + ChatColor.values().filter { it.isColor() }
        val dropdown = DropDown(
            options.indexOf(display.getCurrentSetting(header)),
            options.map {
                if (it is ChatColor)
                    "${it}${it.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }}"
                else it.toString()
            }
        ).constrain {
            x = 0.pixels(true)
            y = 0.pixels
        }.childOf(this).also {
            it.onValueChange {
            when (it) {
                // ignore chroma and rgb options
                0 -> {}
                1 -> {}
                else -> {
                    if (selector.getCurrentColor() == (options[it] as ChatColor).color!!) return@onValueChange
                    val (red, green, blue) = (options[it] as ChatColor).color!!
                    val (h,s,b) = Color.RGBtoHSB(red, green, blue, null)
                    selector.setHSB(h, s, b)
                }
            }
        }
        }
        val selector = if (header) {
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
        } childOf this

        private fun LevelheadDisplay.getCurrentSetting(header: Boolean) = when(this.config.getMode(header)) {
            "Chat Color" -> {
                ChatColor.values().filter { it.isColor() }
                    .find {
                        it.color!!.rgb == if (header) this.config.headerColor.rgb else this.config.footerColor.rgb
                    }
            }
            else -> this.config.getMode(header)
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

    }

    fun parseProposal(uiComponent: UIComponent, label: UIText, request: JsonObject?): Pair<UIText, UIText> {
        val aboveHeadDisplayConfig = Levelhead.displayManager.aboveHead[0].config
        val proposalFooter = UIText(request?.get("strlevel")?.asString?.replace(defaultRegex, "") ?: "").constrain {
            x = 2.5.pixels(true)
            y = CopyConstraintFloat() boundTo label
        } childOf uiComponent
        val proposalHeader = UIText(request?.get("header")?.asString?.replace(defaultRegex, "") ?: "No current proposal").constrain {
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
            Levelhead.jsonParser.parse(Levelhead.rawWithAgent("https://api.sk1er.club/levelheadapi/info?auth=${Levelhead.auth.hash}")).asJsonObject

        private fun setLevelheadColor(headerChroma: Boolean, headerColor: Color, footerChroma: Boolean, footerColor: Color) {
            val colors = JsonObject()
            colors.addProperty("headerChroma", headerChroma)
            colors.addProperty("headerColor", headerColor.withAlpha(1f).rgb)
            colors.addProperty("footerChroma", footerChroma)
            colors.addProperty("footerColor", footerColor.withAlpha(1f).rgb)
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