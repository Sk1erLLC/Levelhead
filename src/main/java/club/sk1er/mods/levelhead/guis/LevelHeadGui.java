package club.sk1er.mods.levelhead.guis;

import club.sk1er.mods.levelhead.LevelHead;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.Sk1erMod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

/**
 * Created by mitchellkatz on 6/10/17.
 *
 * Modified by boomboompower on 14/6/2017
 */
public class LevelHeadGui extends GuiScreen {

    // Recommended GUI display Values (this.height / 2 (sign) value)
    //        - 58
    //        - 34
    //        - 10
    //        + 14
    //        + 38
    //        + 62

    private final String ENABLED = ChatColor.GREEN + "Enabled";
    private final String DISABLED = ChatColor.RED + "Disabled";
    private final String COLOR_CHAR = String.valueOf("\u00a7");

    private final String colors = "0123456789abcdef";

    private Minecraft mc;

    private GuiButton textButton;
    private GuiButton levelButton;
    private GuiButton prefixButton;

    private GuiTextField textField;

    public LevelHeadGui() {
        mc = Minecraft.getMinecraft();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        this.buttonList.add(new GuiButton(1, this.width / 2 - 100, this.height / 2 - 34, 200, 20, "LevelHead: " + getLevelToggle()));

        this.buttonList.add(new GuiButton(2, this.width / 2 - 155, this.height / 2 - 10, 150, 20, "Text Chroma: " + getChromaToggle(true)));
        this.buttonList.add(new GuiButton(3, this.width / 2 + 5, this.height / 2 - 10, 150, 20, "Level Chroma: " + getChromaToggle(false)));

        this.buttonList.add(this.textButton = new GuiButton(4, this.width / 2 - 155, this.height / 2 + 14, 150, 20, "Rotate"));
        this.buttonList.add(this.levelButton = new GuiButton(5, this.width / 2 + 5, this.height / 2 + 14, 150, 20, "Rotate"));

        this.buttonList.add(this.prefixButton = new GuiButton(6, this.width / 2 - 100, this.height / 2 + 74, 200, 20, "Set Prefix"));

        this.textField = new GuiTextField(0, mc.fontRendererObj, this.width / 2 - 75, this.height / 2 + 44, 150, 20);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ticks) {
        drawDefaultBackground();
        drawTitle("Sk1er LevelHead v" + LevelHead.VERSION);
        drawLook();

        textField.drawTextBox();

        textButton.enabled = !LevelHead.PRIMARY_CHROMA;
        levelButton.enabled = !LevelHead.SECONDARY_CHROMA;
        prefixButton.enabled = !textField.getText().isEmpty();

        for (GuiButton aButtonList : this.buttonList) {
            aButtonList.drawButton(this.mc, mouseX, mouseY);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                LevelHead.TOGGLED_ON = !LevelHead.TOGGLED_ON;
                button.displayString = "LevelHead: " + getLevelToggle();

                sendChatMessage(String.format("Toggled %s!", (LevelHead.TOGGLED_ON ? "on" : "off")));
                break;
            case 2:
                LevelHead.PRIMARY_CHROMA = !LevelHead.PRIMARY_CHROMA;
                button.displayString = "Text Chroma: " + getChromaToggle(true);
                break;
            case 3:
                LevelHead.SECONDARY_CHROMA = !LevelHead.SECONDARY_CHROMA;
                button.displayString = "Level Chroma: " + getChromaToggle(false);
                break;
            case 4:
                int primaryId = colors.indexOf(removeColorChar(LevelHead.PRIMARY_COLOR));
                if (++primaryId == colors.length()) {
                    primaryId = 0;
                }
                LevelHead.PRIMARY_COLOR = COLOR_CHAR + colors.charAt(primaryId);
                break;
            case 5:
                int secondaryId = colors.indexOf(removeColorChar(LevelHead.SECOND_COLOR));
                if (++secondaryId == colors.length() - 1) {
                    secondaryId = 0;
                }
                LevelHead.SECOND_COLOR = COLOR_CHAR + colors.charAt(secondaryId);
                break;
            case 6:
                changePrefix();
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
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
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
        LevelHead.getInstance().saveConfig();
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
            LevelHead.PREFIX = textField.getText();
            LevelHead.stringCache.clear();
            sendChatMessage(String.format("LevelHead prefix is now %s!", ChatColor.GOLD + textField.getText() + ChatColor.YELLOW));
        } else {
            sendChatMessage("No prefix supplied!");
        }
        mc.displayGuiScreen(null);
    }

    private void drawTitle(String text) {
        drawCenteredString(mc.fontRendererObj, text, this.width / 2, this.height / 2 - 80, Color.WHITE.getRGB());
        drawHorizontalLine(this.width / 2 - mc.fontRendererObj.getStringWidth(text) / 2 - 5, this.width / 2 + mc.fontRendererObj.getStringWidth(text) / 2 + 5, this.height / 2 - 70, Color.WHITE.getRGB());
    }

    private void drawLook() {
        FontRenderer renderer = mc.fontRendererObj;
        if (LevelHead.TOGGLED_ON) {
            drawCenteredString(renderer, "This is how levels will display", this.width / 2, this.height / 2 - 60, Color.WHITE.getRGB());
            drawCenteredString(renderer, String.format("%s%s: ", (LevelHead.PRIMARY_CHROMA ? "" : LevelHead.PRIMARY_COLOR), LevelHead.PREFIX), this.width / 2, this.height / 2 - 50, (LevelHead.PRIMARY_CHROMA ? LevelHead.getColor() : Color.WHITE.getRGB()));
            drawCenteredString(renderer, String.format("%s5", (LevelHead.SECONDARY_CHROMA ? "" : LevelHead.SECOND_COLOR)), (this.width / 2 + renderer.getStringWidth(LevelHead.PREFIX + ": ") / 2 + 3), this.height / 2 - 50, (LevelHead.SECONDARY_CHROMA ? LevelHead.getColor() : Color.WHITE.getRGB()));
        } else {
            drawCenteredString(renderer, "LevelHead is disabled", this.width / 2, this.height / 2 - 60, Color.WHITE.getRGB());
            drawCenteredString(renderer, "Player level\'s will not appear", this.width / 2, this.height / 2 - 50, Color.WHITE.getRGB());
        }
    }

    private String getLevelToggle() {
        return LevelHead.TOGGLED_ON ? ENABLED : DISABLED;
    }

    private String getChromaToggle(boolean text) {
        return (text ? (LevelHead.PRIMARY_CHROMA ? ENABLED : DISABLED) : (LevelHead.SECONDARY_CHROMA ? ENABLED : DISABLED));
    }

    private String removeColorChar(String message) {
        return message.replace(COLOR_CHAR, "");
    }
}