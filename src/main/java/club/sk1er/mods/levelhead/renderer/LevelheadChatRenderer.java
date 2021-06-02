package club.sk1er.mods.levelhead.renderer;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.DisplayConfig;
import club.sk1er.mods.levelhead.display.LevelheadDisplay;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class LevelheadChatRenderer {

    private final Levelhead levelhead;
    private final Pattern UUID_PATTERN = Pattern.compile("/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i");

    public LevelheadChatRenderer(Levelhead levelhead) {
        this.levelhead = levelhead;
    }

    public static IChatComponent modifyChat(IChatComponent component, String tag, DisplayConfig config) {
        ChatComponentText text = new ChatComponentText(config.getHeaderColor() + "[" + config.getFooterColor() +
            tag +
            config.getHeaderColor() + "]" + EnumChatFormatting.RESET);
        text.appendSibling(component);
        return text;
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent event) {
        if (!levelhead.getDisplayManager().getMasterConfig().isEnabled()) {
            return;
        }
        LevelheadDisplay chat = Levelhead.getInstance().getDisplayManager().getChat();
        if (chat == null) {
            return;
        }
        if (!levelhead.getLevelheadPurchaseStates().isChat()) {
            return;
        }
        if (!chat.getConfig().isEnabled())
            return;
        //#if MC<=10809
        List<IChatComponent> siblings = event.message.getSiblings();
        //#else
        //$$ List<ITextComponent> siblings = event.getMessage().getSiblings();
        //#endif
        if (siblings.size() == 0) {
            return;
        }

        IChatComponent chatComponent = siblings.get(0);
        if (chatComponent instanceof ChatComponentText) {
            ChatStyle chatStyle = chatComponent.getChatStyle();
            ClickEvent chatClickEvent = chatStyle.getChatClickEvent();
            if (chatClickEvent != null) {
                if (chatClickEvent.getAction() == ClickEvent.Action.RUN_COMMAND) {
                    String value = chatClickEvent.getValue();
                    HoverEvent chatHoverEvent = chatStyle.getChatHoverEvent();
                    if (chatHoverEvent != null) {
                        if (chatHoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT) {
                            String[] split = value.split(" ");
                            if (split.length == 2) {
                                String uuid = split[1];
                                if (!UUID_PATTERN.matcher(uuid).matches()) return;
                                UUID key = UUID.fromString(uuid);
                                String tag = chat.getTrueValueCache().get(key);
                                if (tag != null) {
                                    //#if MC<=10809
                                    event.message = modifyChat(event.message, tag, chat.getConfig());
                                    //#else
                                    //$$  event.setMessage(modifyChat(event.getMessage(), tag, chat.getConfig()));
                                    //#endif
                                } else {
                                    if (!(chat.getCache().get(key) instanceof NullLevelheadTag)) {
                                        levelhead.fetch(key, chat, false);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}
