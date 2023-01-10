package club.sk1er.mods.levelhead.render

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.core.tryToGetChatColor
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.api.EssentialAPI
import gg.essential.universal.ChatColor
import gg.essential.universal.utils.MCITextComponent
import gg.essential.universal.wrappers.UPlayer
import gg.essential.universal.wrappers.message.UTextComponent
import kotlinx.coroutines.launch
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

object ChatRender {

    val uuidRegex = Regex(
        "^([0-9a-f]{8})-([0-9a-f]{4})-([0-9a-f]{4})-([0-9a-f]{4})-([0-9a-f]{12})$",
        RegexOption.IGNORE_CASE
    )

    fun modifyChat(component: MCITextComponent, tag: String, config: DisplayConfig) =
        if (Levelhead.LevelheadPurchaseStates.chat && config.enabled &&
            (UPlayer.getPlayer()?.name?.let { component.formattedText.substringBefore(':').contains(it) } == false
                    || config.showSelf)
        ) {
            UTextComponent(
                "${config.headerColor.tryToGetChatColor()}[" +
                        "${config.footerColor.tryToGetChatColor()}$tag" +
                        "${config.headerColor.tryToGetChatColor()}] ${ChatColor.RESET}"
            ).appendSibling(component)
        } else component

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        if (listOf(
                !Levelhead.displayManager.config.enabled,
                !Levelhead.displayManager.chat.config.enabled,
                !Levelhead.LevelheadPurchaseStates.chat,
                !event.message.formattedText.contains(':')
        ).any { it }) return

        val siblings = event.message.siblings

        if (siblings.isEmpty()) return

        val first = siblings.firstOrNull()
        if (listOf(
                first is ChatComponentText,
                //#if MC==10809
                first?.chatStyle?.chatClickEvent?.action == ClickEvent.Action.RUN_COMMAND,
                first?.chatStyle?.chatHoverEvent?.action == HoverEvent.Action.SHOW_TEXT
                //#else
                //$$ first?.style?.clickEvent?.action == ClickEvent.Action.RUN_COMMAND,
                //$$ first?.style?.hoverEvent?.action == HoverEvent.Action.SHOW_TEXT
                //#endif
        ).all { it }) {
            //#if MC==10809
            val split = first!!.chatStyle.chatClickEvent.value.split(' ')
            //#else
            //$$ val split = first!!.style.clickEvent!!.value.split(' ')
            //#endif
            if (split.size == 2) {
                EssentialAPI.getMojangAPI().getUUID(split[1])?.thenAccept { uuid ->
                    Levelhead.displayManager.chat.cache[uuid]?.run {
                        event.message = modifyChat(event.message, this.footer.value, Levelhead.displayManager.chat.config)
                    } ?: Levelhead.scope.launch {
                        Levelhead.fetch(listOf(Levelhead.LevelheadRequest(uuid.trimmed, Levelhead.displayManager.chat, false)))
                    }
                }
            }
        }
    }
}