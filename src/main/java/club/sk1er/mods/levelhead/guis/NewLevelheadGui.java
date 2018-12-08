package club.sk1er.mods.levelhead.guis;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.AboveHeadDisplay;
import club.sk1er.mods.levelhead.display.ChatDisplay;
import club.sk1er.mods.levelhead.display.DisplayConfig;
import club.sk1er.mods.levelhead.display.LevelheadDisplay;
import club.sk1er.mods.levelhead.forge.transform.Hooks;
import club.sk1er.mods.levelhead.purchases.LevelheadPurchaseStates;
import club.sk1er.mods.levelhead.renderer.LevelheadChatRenderer;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import club.sk1er.mods.levelhead.utils.Multithreading;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;
import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class NewLevelheadGui extends GuiScreen implements GuiYesNoCallback {

    private final String colors = "0123456789abcdef";
    private final String COLOR_CHAR = String.valueOf("\u00a7");
    private List<GuiButton> sliders = new ArrayList<>();
    private HashMap<GuiButton, Consumer<GuiButton>> clicks = new HashMap<>();
    private HashMap<Integer, Runnable> ids = new HashMap<>();
    private GuiButton refresh_all_purchase_states = new GuiButton(1, 1, 1, 150, 20, "Refresh All Purchase States");
    private LevelheadDisplay currentlyBeingEdited;
    private boolean bigChange = false;
    private boolean isCustom = false;

    @Override
    public void initGui() {
        Minecraft.getMinecraft().gameSettings.hideGUI = true;
        Multithreading.runAsync(() -> {
            String raw = Sk1erMod.getInstance().rawWithAgent("https://api.sk1er.club/levelhead/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", ""));
            System.out.println(raw);
            this.isCustom = new JsonHolder(raw).optBoolean("custom");
        });
    }

    @Override
    public void onGuiClosed() {
        Minecraft.getMinecraft().gameSettings.hideGUI = false;
        if (bigChange) {
            Levelhead.getInstance().getDisplayManager().clearCache();
        }
        Levelhead.getInstance().getDisplayManager().save();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Levelhead instance = Levelhead.getInstance();

        int currentID = 2;
        buttonList.clear();
        drawRect(0, 0, width, height, Integer.MIN_VALUE);
        reg(refresh_all_purchase_states, button -> {
            Multithreading.runAsync(() -> {
                button.enabled = false;
                button.displayString = "Refreshing....";
                Levelhead.getInstance().refreshPurchaseStates();
                Levelhead.getInstance().refreshPaidData();
                Levelhead.getInstance().refreshRawPurchases();
                button.displayString = "Refresh All Purchase States";
                button.enabled = true;
            });
        });
        reg(new GuiButton(++currentID, 1, 22, 150, 20, "Mod Status: " + (instance.getDisplayManager().getMasterConfig().isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled")), button -> {
            instance.getDisplayManager().getMasterConfig().setEnabled(!instance.getDisplayManager().getMasterConfig().isEnabled());
            button.displayString = "Mod Status: " + (instance.getDisplayManager().getMasterConfig().isEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled");
        });

        reg(new GuiButton(++currentID, 1, 44, 150, 20, "Show above own head: " + (instance.getDisplayManager().getMasterConfig().isShowSelf() ? EnumChatFormatting.GREEN + "Yes" : EnumChatFormatting.RED + "No")), button -> {
            instance.getDisplayManager().getMasterConfig().setShowSelf(!instance.getDisplayManager().getMasterConfig().isShowSelf());
            button.displayString = "Show above own head: " + (instance.getDisplayManager().getMasterConfig().isShowSelf() ? EnumChatFormatting.GREEN + "No" : EnumChatFormatting.RED + "No");
        });

        ScaledResolution current = new ScaledResolution(Minecraft.getMinecraft());

        drawCenteredString(fontRendererObj, EnumChatFormatting.AQUA + "Levelhead Credits: " + EnumChatFormatting.YELLOW + instance.getRawPurchases().optInt("remaining_levelhead_credits"), width / 2, 15, Color.WHITE.getRGB());
        LevelheadPurchaseStates levelheadPurchaseStates = instance.getLevelheadPurchaseStates();
        EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;

        //Fake tab
        String formattedText = thePlayer.getDisplayName().getFormattedText();
        NetworkPlayerInfo playerInfo = Minecraft.getMinecraft().getNetHandler().getPlayerInfo(thePlayer.getUniqueID());
        LevelheadDisplay tab = instance.getDisplayManager().getTab();
        FontRenderer fontRenderer = Minecraft.getMinecraft().fontRendererObj;
        if (tab != null) {
            boolean enabled = tab.getConfig().isEnabled();

            tab.getConfig().setEnabled(true);


            int totalTabWith = 9 + fontRendererObj.getStringWidth(formattedText) + Hooks.getLevelheadWith(playerInfo) + 15;
            int fakeTabTop = 30;
            int leftStart = width / 2 - totalTabWith / 2;
            drawRect(leftStart, fakeTabTop, width / 2 + totalTabWith / 2, fakeTabTop + 8, Integer.MIN_VALUE);
            drawRect(leftStart, fakeTabTop, width / 2 + totalTabWith / 2, fakeTabTop + 8, 553648127);
            fontRenderer.drawString(formattedText, leftStart + 9, fakeTabTop, Color.WHITE.getRGB(), true);

            this.mc.getTextureManager().bindTexture(thePlayer.getLocationSkin());
            int l2 = 8;
            int i3 = 8;
            Gui.drawScaledCustomSizeModalRect(leftStart, fakeTabTop, 8.0F, (float) l2, 8, i3, 8, 8, 64.0F, 64.0F);

            if (thePlayer.isWearing(EnumPlayerModelParts.HAT)) {
                int j3 = 8;
                int k3 = 8;
                Gui.drawScaledCustomSizeModalRect(leftStart, fakeTabTop, 40.0F, (float) j3, 8, k3, 8, 8, 64.0F, 64.0F);
            }
            drawPing(leftStart + totalTabWith, fakeTabTop, playerInfo);
            Hooks.drawPingHook(0, leftStart + totalTabWith, fakeTabTop, playerInfo);

            if (!levelheadPurchaseStates.isTab()) {
                String text = "Levelhead tab display not purchased!\nPlease purchase to activate and configure";
                int i = 1;
                for (String s : text.split("\n")) {
                    int offset = i * 10;
                    int stringWidth = fontRenderer.getStringWidth(s);
                    int tmpLeft = width / 2 - stringWidth / 2;
                    drawRect(tmpLeft, fakeTabTop + offset, tmpLeft + stringWidth, fakeTabTop + offset + 8, Integer.MIN_VALUE);
                    fontRenderer.drawString(s, tmpLeft, fakeTabTop + offset, Color.YELLOW.getRGB(), true);
                    i++;
                }
                reg(new GuiButton(++currentID, leftStart + totalTabWith, fakeTabTop - 10, 120, 20, "Purchase Tab Display"), button -> {
                    this.attemptPurchase("tab");
                });
            } else {
                reg(new GuiButton(++currentID, width / 2 - 60, fakeTabTop + 10, 120, 20, "Edit Tab Display"), button -> {
                    this.currentlyBeingEdited = tab;
                });
            }
            tab.getConfig().setEnabled(enabled);


        }
        //Fake chat
        IChatComponent component = new ChatComponentText(formattedText + EnumChatFormatting.WHITE + ": Levelhead rocks!");
        String chatTag = "Some Stat";
        LevelheadDisplay chatDisplay = instance.getDisplayManager().getChat();
        if (chatDisplay != null && levelheadPurchaseStates.isChat()) {
            chatTag = chatDisplay.getTrueValueCache().get(thePlayer.getUniqueID());
        }
        IChatComponent chatComponent = LevelheadChatRenderer.modifyChat(component, chatTag, chatDisplay != null ? chatDisplay.getConfig() : new DisplayConfig());

        String fakeChatText = chatComponent.getFormattedText();

        int fakeChatTop = height / 2 + 40;
        drawRect(5, fakeChatTop, 5 + fontRenderer.getStringWidth(fakeChatText), height / 2 + 48, Integer.MIN_VALUE);
        fontRenderer.drawString(fakeChatText, 5, fakeChatTop, Color.WHITE.getRGB(), true);

        if (!levelheadPurchaseStates.isChat()) {
            String text = "Levelhead chat display not purchased!\nPlease purchase to activate and configure";
            int i = 2;
            for (String s : text.split("\n")) {
                int offset = i * 10;
                drawRect(5, fakeChatTop + offset, 5 + fontRenderer.getStringWidth(s), height / 2 + 48 + offset, Integer.MIN_VALUE);
                fontRenderer.drawString(s, 5, fakeChatTop + offset, Color.YELLOW.getRGB(), true);
                i++;
            }
            reg(new GuiButton(++currentID, 5, fakeChatTop + (i * 10), 120, 20, "Purchase Chat Display"), button -> {
                this.attemptPurchase("chat");
            });
        } else {
            reg(new GuiButton(++currentID, 5, fakeChatTop + 10, 120, 20, "Edit Chat Display"), button -> {
                this.currentlyBeingEdited = instance.getDisplayManager().getChat();
            });
        }

        reg(new GuiButton(++currentID, width / 2 - 100, height / 2 + 60, 200, 20, "Purchase Additional Above Head Layer"), button -> {
            attemptPurchase("head");
        });

        List<AboveHeadDisplay> aboveHead = instance.getDisplayManager().getAboveHead();
        reg(new GuiButton(++currentID, width / 2 - 100, height / 2 + 81, 200, 20, "Edit Above Head Displays"), button -> {
            currentlyBeingEdited = aboveHead.get(0);
        });

        if (currentlyBeingEdited != null) {
            DisplayConfig config = currentlyBeingEdited.getConfig();


            reg(new GuiButton(++currentID, width - 151, 29, 150, 20, "Status: " + (config.isEnabled() ? "Enabled" : "Disabled")), button -> {
                config.setEnabled(!config.isEnabled());
                button.displayString = "Status: " + (config.isEnabled() ? "Enabled" : "Disabled");
            });

            reg(new GuiButton(++currentID, width - 151, 50, 150, 20, "Type: " + config.getType()), button -> {
                String currentType = config.getType();
                HashMap<String, String> typeMap = instance.allowedTypes();
                Set<String> keys = typeMap.keySet();
                List<String> strings = new ArrayList<>(keys);
                strings.sort(String::compareTo);
                int i = strings.indexOf(config.getType()) + 1;
                if (i >= strings.size())
                    i = 0;

                config.setType(strings.get(i));

                if (config.getCustomHeader().equalsIgnoreCase(typeMap.get(currentType))) {
                    config.setCustomHeader(typeMap.get(strings.get(i)));
                }
                currentlyBeingEdited.getCache().remove(thePlayer.getUniqueID());
                currentlyBeingEdited.getTrueValueCache().remove(thePlayer.getUniqueID());
                bigChange = true;
            });
            if (currentlyBeingEdited instanceof AboveHeadDisplay) {
                reg(new GuiButton(++currentID, width - 151, 72, 150, 20, "Editing layer: " + (aboveHead.indexOf(((AboveHeadDisplay) currentlyBeingEdited)) + 1)), button -> {
                    int i = aboveHead.indexOf(((AboveHeadDisplay) currentlyBeingEdited));
                    i++;
                    if (i >= aboveHead.size()) {
                        i = 0;
                    }
                    currentlyBeingEdited = aboveHead.get(i);
                    button.displayString = "Editing layer: " + (i + 1);
                });


            }
            if (currentlyBeingEdited instanceof ChatDisplay) {
                reg(new GuiButton(++currentID, width - 151, 71, 150, 20, "Rotate Bracket Color"), button -> {
                    int primaryId = colors.indexOf(removeColorChar(config.getHeaderColor()));
                    if (++primaryId == colors.length()) {
                        primaryId = 0;
                    }
                    config.setHeaderColor(COLOR_CHAR + colors.charAt(primaryId));
                });
                reg(new GuiButton(++currentID, this.width - 151, 92, 150, 20, "Rotate Value Color"), button -> {
                    int primaryId = colors.indexOf(removeColorChar(config.getFooterColor()));
                    if (++primaryId == colors.length()) {
                        primaryId = 0;
                    }
                    config.setFooterColor(COLOR_CHAR + colors.charAt(primaryId));
                });
            }

        }

        //Draws the player
        GlStateManager.pushMatrix();
        GlStateManager.color(1, 1, 1);
        GlStateManager.translate(current.getScaledWidth() / 2, current.getScaledHeight() / 2, 5);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.enableDepth();
        GlStateManager.translate(0, 50, 0);
        GlStateManager.translate(0, 0, -50);
        GuiInventory.drawEntityOnScreen(0, 0, 50, 0, 0, thePlayer);
        GlStateManager.depthFunc(515);
        GlStateManager.resetColor();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();

        if (bigChange) {
            int start = height - 25;
            int i = 0;
            for (String s : "Some changes cannot be applied in real time.\nThey will be applied once you close this GUI".split("\n")) {
                fontRenderer.drawString(s, width - fontRenderer.getStringWidth(s) - 5, start + (i * 10), Color.RED.getRGB(), true);
                i++;
            }
        }

        drawScaledText("Custom Levelhead Status: " + (isCustom ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"), width / 2, height / 2 + 105, 1.6, Color.WHITE.getRGB(), true, true);
        reg(new GuiButton(++currentID, this.width / 2 - 155, height / 2 + 125, 310, 20, (isCustom ? ChatColor.YELLOW + "Click to change custom Levelhead." : ChatColor.YELLOW + "Click to purchase a custom Levelhead message")), button -> {
            try {
                if (isCustom) {
                    Desktop.getDesktop().browse(new URI("http://sk1er.club/user"));
                } else {
                    Desktop.getDesktop().browse(new URI("http://sk1er.club/customlevelhead"));
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }

        });
        if (isCustom) {
            GuiButton button1 = new GuiButton(++currentID, this.width / 2 - 155, height / 2 + 125 + 22, 310, 20, ChatColor.YELLOW + "Export these colors to my custom Levelhead");
            reg(button1, button -> {
                JsonHolder object = new JsonHolder();
                object.put("header_obj", instance.getDisplayManager().getAboveHead().get(0).getHeaderConfig());
                object.put("footer_obj", instance.getDisplayManager().getAboveHead().get(0).getFooterConfig());
                try {
                    String encode = URLEncoder.encode(object.toString(), "UTF-8");
                    String url = "https://sk1er.club/user?levelhead_color=" + encode;
                    ChatComponentText text = new ChatComponentText("Click here to update your custom Levelhead colors");
                    ChatStyle style = new ChatStyle();
                    style.setBold(true);
                    style.setColor(EnumChatFormatting.YELLOW);
                    style.setChatClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    ChatComponentText valueIn = new ChatComponentText("Please be logged in to your Sk1er.club for this to work. Do /levelhead dumpcache after clicking to see new colors!");
                    ChatStyle style1 = new ChatStyle();
                    style1.setColor(EnumChatFormatting.RED);
                    valueIn.setChatStyle(style1);
                    style.setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, valueIn));
                    text.setChatStyle(style);
                    Minecraft.getMinecraft().thePlayer.addChatComponentMessage(text);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Minecraft.getMinecraft().displayGuiScreen(null);
            });
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        drawScaledText(EnumChatFormatting.UNDERLINE.toString() + EnumChatFormatting.BOLD + "Sk1er Levelhead " + Levelhead.getInstance().getVersion(), width / 2, 5, 2, Color.WHITE.getRGB(), true, true);

    }

    public void drawScaledText(String text, int trueX, int trueY, double scaleFac, int color, boolean shadow, boolean centered) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFac, scaleFac, scaleFac);
        fontRendererObj.drawString(text, (float) (((double) trueX) / scaleFac) - (centered ? fontRendererObj.getStringWidth(text) / 2 : 0), (float) (((double) trueY) / scaleFac), color, shadow);
        GlStateManager.scale(1 / scaleFac, 1 / scaleFac, 1 / scaleFac);
        GlStateManager.popMatrix();
    }

    private void attemptPurchase(String chat) {
        Levelhead instance = Levelhead.getInstance();
        JsonHolder paidData = instance.getPaidData();
        System.out.println(paidData);
        JsonHolder extra_displays = paidData.optJsonObject("extra_displays");
        JsonHolder stats = paidData.optJsonObject("stats");
        boolean found = false;
        boolean display = false;
        String name = null;
        String description = null;
        int cost = 0;
        boolean single = false;
        if (extra_displays.has(chat)) {
            JsonHolder jsonHolder = extra_displays.optJsonObject(chat);
            found = true;
            display = true;
            name = jsonHolder.optString("name");
            description = jsonHolder.optString("description");
            cost = jsonHolder.optInt("cost");
            single = jsonHolder.optBoolean("single");
        } else if (stats.has(chat)) {
            JsonHolder jsonHolder = stats.optJsonObject(chat);
            found = true;
            name = jsonHolder.optString("name");
            description = jsonHolder.optString("description");
            cost = jsonHolder.optInt("cost");
            single = jsonHolder.optBoolean("single");
        }
        Sk1erMod sk1erMod = Sk1erMod.getInstance();
        int remaining_levelhead_credits = instance.getRawPurchases().optInt("remaining_levelhead_credits");
        if (remaining_levelhead_credits < cost) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            sk1erMod.sendMessage("Insufficient credits! " + name + " costs " + cost + " credits but you only have " + remaining_levelhead_credits);
            sk1erMod.sendMessage("You can purchase more credits here: https://purchase.sk1er.club/category/1050972");
            return;
        }
        if (instance.getAuth().isFailed()) {
            sk1erMod.sendMessage("Could not verify your identify. Please restart the client. If issues persists, contact Sk1er");
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }
        if (found) {
            String finalName = name;
            ids.put(chat.hashCode(), () -> {
                sk1erMod.sendMessage("Attempting to purchase: " + finalName);
                Multithreading.runAsync(() -> {
                    JsonHolder jsonHolder = new JsonHolder(sk1erMod.rawWithAgent("https://api.sk1er.club/levelhead_purchase?access_token=" + instance.getAuth().getAccessKey() + "&request=" + chat + "&hash=" + instance.getAuth().getHash()));
                    if (jsonHolder.optBoolean("success")) {
                        instance.refreshPurchaseStates();
                        sk1erMod.sendMessage("Successfully purchased: " + finalName);
                    } else {
                        sk1erMod.sendMessage("Failed to purchase: " + finalName + ". Cause: " + jsonHolder.optString("cause"));

                    }
                });

            });
            GuiYesNo gui = new GuiYesNo(this, "Purchase " + finalName, "Description: " + description + ". This item may be purchased " + (single ? "one time" : "many times") + ". Type: " + (display ? "Display" : "Extra Stat"), "Purchase for " + cost + " credits", "Cancel", chat.hashCode());
            Minecraft.getMinecraft().displayGuiScreen(gui);
        } else {
            sk1erMod.sendMessage("Could not find package: " + chat + ". Please contact Sk1er immediately");
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        super.confirmClicked(result, id);
        if (result) {
            Runnable runnable = ids.get(id);
            if (runnable != null) {
                runnable.run();
            }
        }
        Minecraft.getMinecraft().displayGuiScreen(this);
    }

    protected void drawPing(int x, int y, NetworkPlayerInfo networkPlayerInfoIn) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(icons);
        int i = 0;
        int j = 0;

        if (networkPlayerInfoIn.getResponseTime() < 0) {
            j = 5;
        } else if (networkPlayerInfoIn.getResponseTime() < 150) {
            j = 0;
        } else if (networkPlayerInfoIn.getResponseTime() < 300) {
            j = 1;
        } else if (networkPlayerInfoIn.getResponseTime() < 600) {
            j = 2;
        } else if (networkPlayerInfoIn.getResponseTime() < 1000) {
            j = 3;
        } else {
            j = 4;
        }

        this.zLevel += 100.0F;
        this.drawTexturedModalRect(x - 11, y, i * 10, 176 + j * 8, 10, 8);
        this.zLevel -= 100.0F;
    }

    public void display() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        MinecraftForge.EVENT_BUS.unregister(this);
        Minecraft.getMinecraft().displayGuiScreen(this);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Consumer<GuiButton> guiButtonConsumer = clicks.get(button);
        if (guiButtonConsumer != null) {
            guiButtonConsumer.accept(button);
        }
    }

    private void regSlider(net.minecraftforge.fml.client.config.GuiSlider slider, Consumer<GuiButton> but) {
        reg(slider, but);
        sliders.add(slider);

    }

    private void reg(GuiButton button, Consumer<GuiButton> consumer) {
        this.buttonList.removeIf(button1 -> button1.id == button.id);
        this.clicks.keySet().removeIf(button1 -> button1.id == button.id);

        this.buttonList.add(button);
        this.clicks.put(button, consumer);
    }

    private String removeColorChar(String message) {
        return message.replace(COLOR_CHAR, "");
    }

}
