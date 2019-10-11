package club.sk1er.mods.levelhead.guis;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import club.sk1er.mods.levelhead.utils.Multithreading;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.client.config.GuiSlider;

import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.function.Consumer;

import static net.minecraft.util.EnumChatFormatting.BOLD;
import static net.minecraft.util.EnumChatFormatting.UNDERLINE;

/**
 * Created by mitchellkatz on 5/2/18. Designed for production use on Sk1er.club
 */
public class CustomLevelheadConfigurer extends GuiScreen {
    int cooldown = 0;
    int idIteration = 0;
    private GuiTextField header;
    private GuiTextField level;
    private JsonHolder levelhead_propose = new JsonHolder();
    private HashMap<GuiButton, Consumer<GuiButton>> clicks = new HashMap<>();

    public int nextId() {
        return (++idIteration);
    }

    @Override
    public void initGui() {
        super.initGui();

        int i = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth() - 20;
        header = new GuiTextField(nextId(), fontRendererObj, width / 2 - 205, 30, 200, 20);
        level = new GuiTextField(nextId(), fontRendererObj, width / 2 + 5, 30, 200, 20);
        header.setMaxStringLength(50);
        level.setMaxStringLength(50);

        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = new JsonHolder(Sk1erMod.getInstance().rawWithAgent("https://sk1er.club/newlevel/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "")));
            header.setText(jsonHolder.optString("header"));
            level.setText(jsonHolder.optString("true_footer"));
        });
        Multithreading.runAsync(() -> levelhead_propose = new JsonHolder(Sk1erMod.getInstance().rawWithAgent("https://api.hyperium.cc/levelhead_propose/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", ""))));
        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = new JsonHolder(Sk1erMod.getInstance().rawWithAgent("https://api.hyperium.cc/levelhead/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "")));
            if (!jsonHolder.optBoolean("levelhead")) {
                if (Minecraft.getMinecraft().currentScreen instanceof CustomLevelheadConfigurer) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                    Sk1erMod.getInstance().sendMessage("You must purchase Custom Levelhead to use this!");
                }
            }
        });
        refresh();
        reg(new GuiButton(nextId(), width / 2 - 205, 55, 200, 20, "Reset to default"), button -> {
            Sk1erMod.getInstance().rawWithAgent("https://api.sk1er.club/customlevelhead/reset?hash=" + Levelhead.getInstance().getAuth().getHash() + "&level=default&header=default");
            refresh();
        });
        reg(new GuiButton(nextId(), width / 2 + 5, 55, 200, 20, "Send for review"), button -> {
            Sk1erMod.getInstance().rawWithAgent("https://api.sk1er.club/customlevelhead/propose?hash=" + Levelhead.getInstance().getAuth().getHash() + "&footer=" + URLEncoder.encode(level.getText()) + "&header=" + URLEncoder.encode(header.getText()));
            refresh();
        });
        reg(new GuiButton(nextId(), width / 2 - 50, 80, 100, 20, "Refresh"), button -> {
            refresh();
            cooldown = 0;
        });
    }

    public void refresh() {
        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = new JsonHolder(Sk1erMod.getInstance().rawWithAgent("https://sk1er.club/newlevel/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "")));
            header.setText(jsonHolder.optString("header"));
            level.setText(jsonHolder.optString("true_footer"));
        });
        Multithreading.runAsync(() -> {
            levelhead_propose = new JsonHolder(Sk1erMod.getInstance().rawWithAgent("https://api.hyperium.cc/levelhead_propose/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "")));

        });
    }

    private void drawScaledText(String text, int trueX, int trueY, double scaleFac, int color, boolean shadow, boolean centered) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFac, scaleFac, scaleFac);
        fontRendererObj.drawString(text, (float) (((double) trueX) / scaleFac) - (centered ?
                fontRendererObj.getStringWidth(text) / 2F : 0), (float) (((double) trueY) / scaleFac), color, shadow);
        GlStateManager.scale(1 / scaleFac, 1 / scaleFac, 1 / scaleFac);
        GlStateManager.popMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        header.drawTextBox();
        level.drawTextBox();
        drawScaledText(UNDERLINE.toString() + BOLD + "Custom Levelhead Message Configurer", width / 2, 5, 2, new Color(255, 61, 214).getRGB(), true, true);
        if (levelhead_propose.getKeys().size() == 0) {
            drawScaledText(ChatColor.RED + "Loading: Error", width / 2, 115, 1.25, Color.WHITE.getRGB(), true, true);
            return;
        }
        if (levelhead_propose.optBoolean("denied")) {
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.RED + "Denied", width / 2, 115, 1.25, Color.WHITE.getRGB(), true, true);
            return;
        }
        if (levelhead_propose.optBoolean("enabled")) {
            int i = 115;
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "Accepted", width / 2, i - 5, 1.25, Color.WHITE.getRGB(), true, true);
            drawScaledText(ChatColor.YELLOW + "Header: " + ChatColor.AQUA + levelhead_propose.optString("header"), width / 2, 125, 1.25, Color.WHITE.getRGB(), true, true);
            drawScaledText(ChatColor.YELLOW + "Level: " + ChatColor.AQUA + levelhead_propose.optString("strlevel"), width / 2, 140, 1.25, Color.WHITE.getRGB(), true, true);
        } else {
            int i = 115;
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.YELLOW + "Pending", width / 2, i - 5, 1.25, Color.WHITE.getRGB(), true, true);
            drawScaledText(ChatColor.YELLOW + "Header: " + ChatColor.AQUA + levelhead_propose.optString("header"), width / 2, 125, 1.25, Color.WHITE.getRGB(), true, true);
            drawScaledText(ChatColor.YELLOW + "Level: " + ChatColor.AQUA + levelhead_propose.optString("strlevel"), width / 2, 140, 1.25, Color.WHITE.getRGB(), true, true);
            drawScaledText(ChatColor.YELLOW + "It will be reviewed soon!", width / 2, 155, 1.25, Color.WHITE.getRGB(), true, true);

        }

    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        header.mouseClicked(mouseX, mouseY, mouseButton);
        level.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        header.textboxKeyTyped(typedChar, keyCode);
        level.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Consumer<GuiButton> guiButtonConsumer = clicks.get(button);
        if (guiButtonConsumer != null) {
            guiButtonConsumer.accept(button);
        }
    }

    private void regSlider(GuiSlider slider) {
        reg(slider, null);
    }

    private void reg(GuiButton button, Consumer<GuiButton> consumer) {
        this.buttonList.removeIf(button1 -> button1.id == button.id);
        this.clicks.keySet().removeIf(button1 -> button1.id == button.id);
        this.buttonList.add(button);
        if (consumer != null) {
            this.clicks.put(button, consumer);
        }
    }
}
