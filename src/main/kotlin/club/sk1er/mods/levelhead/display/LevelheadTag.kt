package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import com.google.gson.JsonObject
import java.awt.Color
import java.util.*

class LevelheadTag(val owner: UUID) {
    var header: LevelheadComponent = LevelheadComponent()
    var footer: LevelheadComponent = LevelheadComponent()

    fun getString() = "${header.value}${footer.value}"

    override fun toString(): String = "LevelheadTag{header=$header, footer=$footer, owner=$owner}"

    fun clone(): LevelheadTag = Levelhead.gson.fromJson(Levelhead.gson.toJson(this), LevelheadTag::class.java)

    companion object {
        fun build(owner: UUID, block: LevelheadTagBuilder.() -> Unit) =
            LevelheadTagBuilder(owner).apply(block).tag
    }

    class LevelheadTagBuilder(owner: UUID) {
        val tag = LevelheadTag(owner)

        fun header(block: LevelheadComponent.() -> Unit) =
            tag.header.apply(block)

        fun footer(block: LevelheadComponent.() -> Unit) =
            tag.footer.apply(block)
    }

    class LevelheadComponent {
        var value: String = ""
            set(value) { field = value.replace("&", "\u00a7") }
        var color: Color = Color.WHITE
        var chroma: Boolean = false

        override fun toString(): String = "LevelheadComponent{value='$value', color='${color}', chroma=$chroma}"
    }
}