package club.sk1er.mods.levelhead.config

import gg.essential.universal.ChatColor
import org.apache.commons.lang3.text.WordUtils
import java.awt.Color
import java.util.*

class DisplayConfig {
    var enabled: Boolean = true
    var showSelf: Boolean = true
    var type: String = "LEVEL"

    var headerColor: Color = ChatColor.AQUA.color!!
    var headerChroma: Boolean = false
    var headerString: String = WordUtils.capitalizeFully(type)

    var footerColor: Color = ChatColor.YELLOW.color!!
    var footerChroma: Boolean = false
    var footerString: String? = null
}