package club.sk1er.mods.levelhead.guis;

import club.sk1er.mods.levelhead.Levelhead;
import gg.essential.api.EssentialAPI;
import gg.essential.api.utils.JsonHolder;
import gg.essential.api.utils.Multithreading;
import gg.essential.api.utils.WebUtil;
import gg.essential.universal.ChatColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.Color;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.function.Consumer;

import static net.minecraft.util.EnumChatFormatting.BOLD;
import static net.minecraft.util.EnumChatFormatting.UNDERLINE;

public class CustomLevelheadConfigurer extends GuiScreen {
    int cooldown = 0;
    int idIteration = 0;
    private GuiTextField header;
    private GuiTextField level;
    private JsonHolder levelhead_propose = new JsonHolder();
    private final HashMap<GuiButton, Consumer<GuiButton>> clicks = new HashMap<>();

    public int nextId() {
        return (++idIteration);
    }

    @Override
    public void initGui() {
        super.initGui();

        header = new GuiTextField(nextId(), this.fontRendererObj, width / 2 - 205, 30, 200, 20);
        level = new GuiTextField(nextId(), this.fontRendererObj, width / 2 + 5, 30, 200, 20);
        header.setMaxStringLength(50);
        level.setMaxStringLength(50);

        String trimmedUUid = Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "");
        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = WebUtil.fetchJSON("https://sk1er.club/newlevel/" + trimmedUUid);
            header.setText(jsonHolder.optString("header"));
            level.setText(jsonHolder.optString("true_footer"));
        });
        Multithreading.runAsync(() -> levelhead_propose = WebUtil.fetchJSON("https://api.hyperium.cc/levelhead_propose/" + trimmedUUid));
        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = WebUtil.fetchJSON("https://api.hyperium.cc/levelhead/" + trimmedUUid);
            if (!jsonHolder.optBoolean("levelhead")) {
                if (Minecraft.getMinecraft().currentScreen instanceof CustomLevelheadConfigurer) {
                    Minecraft.getMinecraft().displayGuiScreen(null);
                    EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, "You must purchase Custom Levelhead to use this!");
                }
            }
        });
        refresh();
        reg(new GuiButton(nextId(), width / 2 - 205, 55, 200, 20, "Reset to default"), button -> {
            WebUtil.fetchJSON("https://api.sk1er.club/customlevelhead/reset?hash=" + Levelhead.INSTANCE.getAuth().getHash() + "&level=default&header=default");
            refresh();
        });
        reg(new GuiButton(nextId(), width / 2 + 5, 55, 200, 20, "Send for review"), button -> {
            try {
                WebUtil.fetchJSON("https://api.sk1er.club/customlevelhead/propose?hash=" + Levelhead.INSTANCE.getAuth().getHash() + "&footer=" + URLEncoder.encode(level.getText(), "UTF-8") + "&header=" + URLEncoder.encode(header.getText(), "UTF-8"));
                refresh();
            } catch (Exception e) {
                Levelhead.INSTANCE.getLogger().error("Failed to encode {}: {}", level.getText(), header.getText(), e);
            }
        });
        reg(new GuiButton(nextId(), width / 2 - 50, 80, 100, 20, "Refresh"), button -> {
            refresh();
            cooldown = 0;
        });
    }

    public void refresh() {
        String trimmedUuid = Minecraft.getMinecraft().getSession().getProfile().getId().toString().replace("-", "");
        Multithreading.runAsync(() -> {
            JsonHolder jsonHolder = WebUtil.fetchJSON("https://sk1er.club/newlevel/" + trimmedUuid);
            header.setText(jsonHolder.optString("header"));
            level.setText(jsonHolder.optString("true_footer"));
        });
        Multithreading.runAsync(() -> levelhead_propose = WebUtil.fetchJSON("https://api.hyperium.cc/levelhead_propose/" + trimmedUuid));
    }

    private void drawScaledText(String text, int trueX, int trueY, double scaleFac, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.scale(scaleFac, scaleFac, scaleFac);
        this.fontRendererObj.drawString(text, (float) (((double) trueX) / scaleFac) - (this.fontRendererObj.getStringWidth(text) / 2F), (float) (((double) trueY) / scaleFac), color, true);
        GlStateManager.scale(1 / scaleFac, 1 / scaleFac, 1 / scaleFac);
        GlStateManager.popMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        header.drawTextBox();
        level.drawTextBox();
        drawScaledText(UNDERLINE.toString() + BOLD + "Custom Levelhead Message Configurer", width / 2, 5, 2, new Color(255, 61, 214).getRGB());
        if (levelhead_propose.getKeys().size() == 0) {
            drawScaledText(ChatColor.RED + "Loading: Error", width / 2, 115, 1.25, Color.WHITE.getRGB());
            return;
        }
        if (levelhead_propose.optBoolean("denied")) {
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.RED + "Denied", width / 2, 115, 1.25, Color.WHITE.getRGB());
            return;
        }
        if (levelhead_propose.optBoolean("enabled")) {
            int i = 115;
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.GREEN + "Accepted", width / 2, i - 5, 1.25, Color.WHITE.getRGB());
            drawScaledText(ChatColor.YELLOW + "Header: " + ChatColor.AQUA + levelhead_propose.optString("header"), width / 2, 125, 1.25, Color.WHITE.getRGB());
            drawScaledText(ChatColor.YELLOW + "Level: " + ChatColor.AQUA + levelhead_propose.optString("strlevel"), width / 2, 140, 1.25, Color.WHITE.getRGB());
        } else {
            int i = 115;
            drawScaledText(ChatColor.YELLOW + "Status: " + ChatColor.YELLOW + "Pending", width / 2, i - 5, 1.25, Color.WHITE.getRGB());
            drawScaledText(ChatColor.YELLOW + "Header: " + ChatColor.AQUA + levelhead_propose.optString("header"), width / 2, 125, 1.25, Color.WHITE.getRGB());
            drawScaledText(ChatColor.YELLOW + "Level: " + ChatColor.AQUA + levelhead_propose.optString("strlevel"), width / 2, 140, 1.25, Color.WHITE.getRGB());
            drawScaledText(ChatColor.YELLOW + "It will be reviewed soon!", width / 2, 155, 1.25, Color.WHITE.getRGB());

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

    private void reg(GuiButton button, Consumer<GuiButton> consumer) {
        this.buttonList.removeIf(button1 -> button1.id == button.id);
        this.clicks.keySet().removeIf(button1 -> button1.id == button.id);
        this.buttonList.add(button);
        if (consumer != null) {
            this.clicks.put(button, consumer);
        }
    }
}
