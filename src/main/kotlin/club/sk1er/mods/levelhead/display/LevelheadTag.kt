package club.sk1er.mods.levelhead.display

import com.google.gson.JsonObject
import java.awt.Color
import java.util.*

class LevelheadTag(val owner: UUID) {
    lateinit var header: LevelheadComponent
    lateinit var footer: LevelheadComponent

    suspend fun construct(jsonObject: JsonObject) {
        if (!this::header.isInitialized) header = build(jsonObject, true)
        if (!this::footer.isInitialized) footer = build(jsonObject, false)
    }

    private fun build(jsonObject: JsonObject, isHeader: Boolean): LevelheadComponent {
        val seek = if (isHeader) "header" else "footer"
        val json = jsonObject[seek].asJsonObject

        val component = json["string"]?.asString?.let { LevelheadComponent(it) } ?:
            LevelheadComponent("UMM BIG ERROR REPORT TO SK1ER")
        val custom = json["custom"]?.asBoolean == true

        if (custom && isHeader && !json.has("exclude")) {
            component.value += ": "
        }

        component.chroma = json["chroma"].asBoolean

        component.color = Color(json["color"].asInt)

        return component
    }

    fun getString() = "${header.value}${footer.value}"

    override fun toString(): String = "LevelheadTag{header=$header, footer=$footer, owner=$owner}"

    class LevelheadComponent(value: String) {
        var value: String = value.replace("&", "\u00a7")
        var color: Color = Color.WHITE
        var chroma: Boolean = false

        override fun toString(): String = "LevelheadComponent{value='$value', color='${color}', chroma=$chroma}"
    }
}