package club.sk1er.mods.levelhead.guis;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.config.LevelheadConfig;
import club.sk1er.mods.levelhead.renderer.LevelheadComponent;
import club.sk1er.mods.levelhead.renderer.LevelheadTag;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import club.sk1er.mods.levelhead.utils.Multithreading;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiSlider;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.input.Keyboard;

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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Created by mitchellkatz on 6/10/17.
 * <p>
 * Modified by boomboompower on 14/6/2017
 */
public class LevelHeadGui extends GuiScreen {


    private final String ENABLED = ChatColor.GREEN + "Enabled";
    private final String DISABLED = ChatColor.RED + "Disabled";
    private final String COLOR_CHAR = String.valueOf("\u00a7");
    private final String colors = "0123456789abcdef";
    List<GuiButton> sliders = new ArrayList<>();
    private HashMap<GuiButton, Consumer<GuiButton>> clicks = new HashMap<>();
    private Minecraft mc;
    private GuiButton headerColorButton;
    private GuiButton footerColorButton;
    private GuiButton prefixButton;
    private boolean isCustom = false;
    private GuiTextField textField;
    private ReentrantLock lock = new ReentrantLock();
    private GuiButton buttonType;

    public LevelHeadGui() {
        mc = Minecraft.getMinecraft();
    }

    private void reg(GuiButton button, Consumer<GuiButton> consumer) {
        this.buttonList.add(button);
        this.clicks.put(button, consumer);
    }

    private int calculateHeight(int row) {
        return 55 + row * 23;
    }

    @Override
    public void initGui() {
        Multithreading.runAsync(() -> {
            String raw = Sk1erMod.getInstance().rawWithAgent("https://api.sk1er.club/levelhead/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", ""));
            System.out.println(raw);
            this.isCustom = new JsonHolder(raw).optBoolean("custom");
            updateCustom();
        });
        Keyboard.enableRepeatEvents(true);

        Levelhead instance = Levelhead.getInstance();
        LevelheadConfig config = instance.getConfig();
        reg(new GuiButton(1, this.width / 2 - 155, calculateHeight(0), 150, 20, "LevelHead: " + getLevelToggle()), button -> {
            config.setEnabled(!config.isEnabled());
            button.displayString = "LevelHead: " + getLevelToggle();
            sendChatMessage(String.format("Toggled %s!", (config.isEnabled() ? "On" : "Off")));
        });
        reg(new GuiButton(69, this.width / 2 + 5, calculateHeight(0), 150, 20, "Show self: " + (config.isShowSelf() ? ChatColor.GREEN + "On" : ChatColor.RED + "Off")), button -> {
            config.setShowSelf(!config.isShowSelf());
            button.displayString = "Show self: " + (config.isShowSelf() ? ChatColor.GREEN + "On" : ChatColor.RED + "Off");
        });
        //RGB -> Chroma
        //Chroma -> Classic
        //Classic -> RGB
        reg(new GuiButton(2, this.width / 2 - 155, calculateHeight(4), 150, 20, "Header Mode: " + getMode(true)), button -> {
            if (config.isHeaderRgb()) {
                config.setHeaderRgb(false);
                config.setHeaderChroma(true);
            } else if (config.isHeaderChroma()) {
                config.setHeaderRgb(false);
                config.setHeaderChroma(false);
            } else {
                config.setHeaderRgb(true);
                config.setHeaderChroma(false);
            }
            button.displayString = "Header Mode: " + getMode(true);
        });

        reg(new GuiButton(3, this.width / 2 + 5, calculateHeight(4), 150, 20, "Footer Mode: " + getMode(false)), button -> {
            if (config.isFooterRgb()) {
                config.setFooterRgb(false);
                config.setFooterChroma(true);
            } else if (config.isFooterChroma()) {
                config.setFooterRgb(false);
                config.setFooterChroma(false);
            } else {
                config.setFooterRgb(true);
                config.setFooterChroma(false);
            }
            button.displayString = "Header Mode: " + getMode(false);
        });


        reg(this.prefixButton = new GuiButton(6, this.width / 2 + 5, calculateHeight(1), 150, 20, "Set Prefix"), button -> changePrefix());

        this.textField = new GuiTextField(0, mc.fontRendererObj, this.width / 2 - 154, calculateHeight(1), 148, 20);

        //Color rotate
        reg(this.headerColorButton = new GuiButton(4, this.width / 2 - 155, calculateHeight(5), 150, 20, "Rotate Color"), button -> {
            int primaryId = colors.indexOf(removeColorChar(config.getHeaderColor()));
            if (++primaryId == colors.length()) {
                primaryId = 0;
            }
            config.setHeaderColor(COLOR_CHAR + colors.charAt(primaryId));
        });
        reg(this.footerColorButton = new GuiButton(5, this.width / 2 + 5, calculateHeight(5), 150, 20, "Rotate Color"), button -> {
            int primaryId = colors.indexOf(removeColorChar(config.getFooterColor()));
            if (++primaryId == colors.length()) {
                primaryId = 0;
            }
            config.setFooterColor(COLOR_CHAR + colors.charAt(primaryId));
        });
        reg(new GuiSlider(13, this.width / 2 - 155, calculateHeight(2), 150, 20, "Display Distance: ", "", 5, 64, config.getRenderDistance(), false, true, slider -> {
            config.setRenderDistance(slider.getValueInt());
            slider.dragging = false;
        }), null);

        reg(new GuiSlider(14, this.width / 2 + 5, calculateHeight(2), 150, 20, "Cache size: ", "", 150, 5000, config.getPurgeSize(), false, true, slider -> {
            config.setPurgeSize(slider.getValueInt());
            slider.dragging = false;
        }), null);


        JsonHolder types = instance.getTypes();
        reg(this.buttonType = new GuiButton(4, this.width / 2 - 155, calculateHeight(3), 150 * 2 + 10, 20, "Current Type: " + types.optJsonObject(instance.getType()).optString("name")), button -> {
            String currentType = instance.getType();
            List<String> keys = types.getKeys();
            int i = keys.indexOf(currentType);
            i++;
            if (i >= keys.size()) {
                i = 0;
            }
            if (config.getCustomHeader().equalsIgnoreCase(types.optJsonObject(currentType).optString("name"))) {
                config.setCustomHeader(types.optJsonObject(keys.get(i)).optString("name"));
            }
            instance.setType(keys.get(i));
            button.displayString = "Current Type: " + types.optJsonObject(instance.getType()).optString("name");
            Levelhead.getInstance().levelCache.clear();
        });

        //public GuiSlider(int id, int xPos, int yPos, int width, int height, String prefix, String suf, double minVal, double maxVal, double currentVal, boolean showDec, boolean drawStr, ISlider par)
        regSlider(new GuiSlider(6, this.width / 2 - 155, calculateHeight(5), 150, 20, "Header Red: ", "", 0, 255, config.getHeaderRed(), false, true, slider -> {
            config.setHeaderRed(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);
        regSlider(new GuiSlider(7, this.width / 2 - 155, calculateHeight(6), 150, 20, "Header Green: ", "", 0, 255, config.getHeaderGreen(), false, true, slider -> {
            config.setHeaderGreen(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);
        regSlider(new GuiSlider(8, this.width / 2 - 155, calculateHeight(7), 150, 20, "Header Blue: ", "", 0, 255, config.getHeaderBlue(), false, true, slider -> {
            config.setHeaderBlue(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);


        regSlider(new GuiSlider(10, this.width / 2 + 5, calculateHeight(5), 150, 20, "Footer Red: ", "", 0, 255, config.getFooterRed(), false, true, slider -> {
            config.setFooterRed(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);
        regSlider(new GuiSlider(11, this.width / 2 + 5, calculateHeight(6), 150, 20, "Footer Green: ", "", 0, 255, config.getFooterGreen(), false, true, slider -> {
            config.setFooterGreen(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);
        regSlider(new GuiSlider(12, this.width / 2 + 5, calculateHeight(7), 150, 20, "Footer Blue: ", "", 0, 255, config.getFooterBlue(), false, true, slider -> {
            config.setFooterBlue(slider.getValueInt());
            updatePeopleToValues();
            slider.dragging = false;
        }), null);


    }

    private void updateCustom() {
        lock.lock();
        reg(new GuiButton(13, this.width / 2 - 155, calculateHeight(8), 310, 20, (isCustom ? ChatColor.YELLOW + "Click to change custom Levelhead." : ChatColor.YELLOW + "Click to purchase a custom Levelhead message")), button -> {

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
            GuiButton button1 = new GuiButton(16, this.width / 2 - 155, calculateHeight(9), 310, 20, ChatColor.YELLOW + "Export these colors to my custom Levelhead");
            reg(button1, button -> {
                JsonHolder object = new JsonHolder();
                object.put("header_obj", Levelhead.getInstance().getHeaderConfig());
                object.put("footer_obj", Levelhead.getInstance().getFooterConfig());
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
        lock.unlock();
    }

    private void regSlider(net.minecraftforge.fml.client.config.GuiSlider slider, Consumer<GuiButton> but) {
        reg(slider, but);
        sliders.add(slider);

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks) {
        lock.lock();
        drawDefaultBackground();
        drawTitle();
        drawLook();


        textField.drawTextBox();

        headerColorButton.visible = !Levelhead.getInstance().getConfig().isHeaderChroma() && !Levelhead.getInstance().getConfig().isHeaderRgb();
        footerColorButton.visible = !Levelhead.getInstance().getConfig().isFooterChroma() && !Levelhead.getInstance().getConfig().isFooterRgb();
        prefixButton.enabled = !textField.getText().isEmpty();
        if (Levelhead.getInstance().getConfig().isHeaderRgb()) {
            for (GuiButton slider : sliders) {
                if (slider.displayString.contains("Header"))
                    slider.visible = true;

            }
        } else {
            for (GuiButton slider : sliders) {
                if (slider.displayString.contains("Header"))
                    slider.visible = false;

            }
        }
        if (Levelhead.getInstance().getConfig().isFooterRgb()) {
            for (GuiButton slider : sliders) {
                if (slider.displayString.contains("Footer"))
                    slider.visible = true;

            }
        } else {
            for (GuiButton slider : sliders) {
                if (slider.displayString.contains("Footer"))
                    slider.visible = false;

            }
        }

        for (GuiButton aButtonList : this.buttonList) {
            aButtonList.drawButton(this.mc, mouseX, mouseY);
        }
        lock.unlock();
    }

    public String getMode(boolean header) {
        LevelheadConfig config = Levelhead.getInstance().getConfig();
        if (header) {
            return config.isHeaderChroma() ? "Chroma" : config.isHeaderRgb() ? "RGB" : "Classic";
        } else {
            return config.isFooterChroma() ? "Chroma" : config.isFooterRgb() ? "RGB" : "Classic";
        }
    }

    public void updatePeopleToValues() {
        Levelhead.getInstance().levelCache.forEach((uuid, levelheadTag) -> {
            String value = Levelhead.getInstance().getTrueValueCache().get(uuid);
            if (value == null)
                return;
            JsonHolder footer = new JsonHolder().put("level", NumberUtils.isNumber(value) ? Long.parseLong(value) : -1).put("strlevel", value);
            LevelheadTag tag = Levelhead.getInstance().buildTag(footer, uuid);
            levelheadTag.reApply(tag);
        });
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Consumer<GuiButton> guiButtonConsumer = clicks.get(button);
        if (guiButtonConsumer != null) {
            guiButtonConsumer.accept(button);
            //Adjust loaded levelhead names
            updatePeopleToValues();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
        } else if (textField.isFocused() && keyCode == 28) {
            changePrefix();
        } else {
            if (Character.isLetterOrDigit(typedChar) || isCtrlKeyDown() || keyCode == 14) {
                textField.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        textField.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            for (int i = 0; i < this.buttonList.size(); ++i) {
                GuiButton guibutton = this.buttonList.get(i);

                if (guibutton.mousePressed(this.mc, mouseX, mouseY)) {
                    net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre event = new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre(this, guibutton, this.buttonList);
                    if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))
                        break;
                    guibutton = event.button;
                    guibutton.playPressSound(this.mc.getSoundHandler());
                    this.actionPerformed(guibutton);
                    if (this.equals(this.mc.currentScreen))
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Post(this, event.button, this.buttonList));
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        Levelhead.getInstance().getSk1erConfig().save();
    }

    public void display() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        MinecraftForge.EVENT_BUS.unregister(this);
        mc.displayGuiScreen(new LevelHeadGui());
    }

    @Override
    public void sendChatMessage(String msg) {
        Sk1erMod.getInstance().sendMessage(msg);
    }

    private void changePrefix() {
        if (!textField.getText().isEmpty()) {
            Levelhead.getInstance().getConfig().setCustomHeader(textField.getText());
            Levelhead.getInstance().levelCache.clear();
            sendChatMessage(String.format("Levelhead prefix is now %s!", ChatColor.GOLD + textField.getText() + ChatColor.YELLOW));
        } else {
            sendChatMessage("No prefix supplied!");
        }
        mc.displayGuiScreen(null);
    }

    private void drawTitle() {
        String text = "Sk1er LevelHead v" + Levelhead.VERSION;

        drawCenteredString(mc.fontRendererObj, text, this.width / 2, 5, Color.WHITE.getRGB());
        drawHorizontalLine(this.width / 2 - mc.fontRendererObj.getStringWidth(text) / 2 - 5, this.width / 2 + mc.fontRendererObj.getStringWidth(text) / 2 + 5, 15, Color.WHITE.getRGB());
        drawCenteredString(mc.fontRendererObj, ChatColor.YELLOW + "Custom Levelhead Status: " + (isCustom ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled / Inactive"), this.width / 2,
                20, Color.WHITE.getRGB());

    }

    private void drawLook() {
        FontRenderer renderer = mc.fontRendererObj;
        if (Levelhead.getInstance().getConfig().isEnabled()) {
            drawCenteredString(renderer, "This is how levels will display", this.width / 2, 30, Color.WHITE.getRGB());
            LevelheadTag levelheadTag = Levelhead.getInstance().buildTag(new JsonHolder(), null);
            LevelheadComponent header = levelheadTag.getHeader();
            int h = 40;
            if (header.isChroma())
                drawCenteredString(renderer, header.getValue(), this.width / 2, h, Levelhead.getRGBColor());
            else if (header.isRgb()) {
//                GlStateManager.color(header.getRed(), header.getGreen(), header.getBlue(), header.getAlpha());
                drawCenteredString(renderer, header.getValue(), this.width / 2, h, new Color(header.getRed(), header.getGreen(), header.getBlue(), header.getAlpha()).getRGB());

            } else {
                drawCenteredString(renderer, header.getColor() + header.getValue(), this.width / 2, h, Color.WHITE.getRGB());
            }

            LevelheadComponent footer = levelheadTag.getFooter();
            footer.setValue("5");
            if (footer.isChroma())
                drawCenteredString(renderer, footer.getValue(), (this.width / 2 + renderer.getStringWidth(header.getValue()) / 2 + 3), h, Levelhead.getRGBColor());
            else if (footer.isRgb()) {
                drawCenteredString(renderer, footer.getValue(), (this.width / 2 + renderer.getStringWidth(header.getValue()) / 2 + 3), h, new Color(footer.getRed(), footer.getBlue(), footer.getGreen(), footer.getAlpha()).getRGB());
            } else {
                drawCenteredString(renderer, footer.getColor() + footer.getValue(), (this.width / 2 + renderer.getStringWidth(header.getValue()) / 2 + 3), h, Color.WHITE.getRGB());
            }


        } else {
            drawCenteredString(renderer, "LevelHead is disabled", this.width / 2, 30, Color.WHITE.getRGB());
            drawCenteredString(renderer, "Player level\'s will not appear", this.width / 2, 40, Color.WHITE.getRGB());
        }
    }

    private String getLevelToggle() {
        return Levelhead.getInstance().getConfig().isEnabled() ? ENABLED : DISABLED;
    }

    private String removeColorChar(String message) {
        return message.replace(COLOR_CHAR, "");
    }
}
