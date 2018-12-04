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

public class LevelheadChatRenderer {

    private boolean inLobby = true;
    private Levelhead levelhead;

    public LevelheadChatRenderer(Levelhead levelhead) {
        this.levelhead = levelhead;
    }

    @SubscribeEvent
    public void chat(ClientChatReceivedEvent event) {
        if (levelhead.getDisplayManager().getMasterConfig().isEnabled()) {
            //TODO more checks to see if state is right and it is purchased
        }
        if (inLobby) {
            LevelheadDisplay chat = Levelhead.getInstance().getDisplayManager().getChat();
            if (chat == null) {
                System.out.println("caht null");
                return;
            }
            List<IChatComponent> siblings = event.message.getSiblings();
            if (siblings.size() > 0) {
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
                                        String tag = chat.getTrueValueCache().get(UUID.fromString(uuid));
                                        if (tag != null) {
                                            DisplayConfig config = chat.getConfig();
                                            ChatComponentText text = new ChatComponentText(config.getHeaderColor() + "[" + config.getFooterColor() +
                                                    tag +
                                                    config.getHeaderColor() + "]" + EnumChatFormatting.RESET);
                                            text.appendSibling(event.message);
                                            event.message = text;
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
}
