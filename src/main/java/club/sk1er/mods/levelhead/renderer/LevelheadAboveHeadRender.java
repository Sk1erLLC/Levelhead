package club.sk1er.mods.levelhead.renderer;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.AboveHeadDisplay;
import club.sk1er.mods.levelhead.gui.LevelheadMainGUI;
import gg.essential.api.EssentialAPI;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class LevelheadAboveHeadRender {

    private final Levelhead levelhead;

    public LevelheadAboveHeadRender(Levelhead levelhead) {
        this.levelhead = levelhead;
    }

    @SubscribeEvent
    public void render(RenderLivingEvent.Specials.Post<EntityLivingBase> event) {
        if (levelhead == null
            || levelhead.getDisplayManager() == null
            || levelhead.getDisplayManager().getMasterConfig() == null
            || !levelhead.getDisplayManager().getMasterConfig().isEnabled()
            || !(EssentialAPI.getMinecraftUtil().isHypixel() || Minecraft.getMinecraft().currentScreen instanceof LevelheadMainGUI)
            || Minecraft.getMinecraft().gameSettings.hideGUI) {
            return;
        }

        //#if MC<=10809
        if (!(event.entity instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) event.entity;
        //#else
        //$$ if (!(event.getEntity() instanceof EntityPlayer)) return;
        //$$ EntityPlayer player = (EntityPlayer) event.getEntity();
        //#endif
        int o = 0;
        for (AboveHeadDisplay display : levelhead.getDisplayManager().getAboveHead()) {
            int index = display.getIndex();
            int extraHead = levelhead.getLevelheadPurchaseStates().getExtraHead();
            if (index > extraHead || !display.getConfig().isEnabled()) continue;
            LevelheadTag levelheadTag = display.getCache().get(player.getUniqueID());

            if (display.loadOrRender(player) && levelheadTag != null && !(levelheadTag instanceof NullLevelheadTag)) {
                if ((player.getUniqueID().equals(Levelhead.INSTANCE.userUuid) && !display.getConfig().isShowSelf()))
                    continue;

                if (player.getDistanceSqToEntity(UMinecraft.getPlayer()) < 4096) {
                    double offset = 0.3;
                    Scoreboard scoreboard = player.getWorldScoreboard();
                    ScoreObjective scoreObjective = scoreboard.getObjectiveInDisplaySlot(2);

                    if ((scoreObjective != null) && (player.getDistanceSqToEntity(UMinecraft.getPlayer()) < 100)) {
                        offset *= 2;
                    }
                    if (player.getUniqueID().equals(Levelhead.INSTANCE.userUuid)) offset = 0;
                    offset += levelhead.getDisplayManager().getMasterConfig().getOffset();
                    //#if MC<=10809
                    renderName(levelheadTag, player, event.x, event.y + offset + o * .3D, event.z);
                    //#else
                    //$$ renderName(levelheadTag, player, event.getX(), event.getY() + offset + o * .3D, event.getZ());
                    //#endif
                }
            }
            o++;
        }

    }

    public void renderName(LevelheadTag tag, EntityPlayer entityIn, double x, double y, double z) {
        FontRenderer fontrenderer = UMinecraft.getFontRenderer();
        float textScale = 0.016666668F * (float) (1.6F * Levelhead.INSTANCE.getDisplayManager().getMasterConfig().getFontSize());
        GlStateManager.pushMatrix();

        int xMultiplier = 1;
        final Minecraft mc = UMinecraft.getMinecraft();
        if (mc.gameSettings != null && mc.gameSettings.thirdPersonView == 2) {
            xMultiplier = -1;
        }

        GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
        if (mc.currentScreen instanceof LevelheadMainGUI) {
            GlStateManager.translate(0.0, -y * 0.5 - 0.2f, 0.0);
        }
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        final RenderManager renderManager = mc.getRenderManager();
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX * xMultiplier, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-textScale, -textScale, textScale);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int stringWidth = fontrenderer.getStringWidth(tag.getString()) >> 1;

        if (mc.currentScreen instanceof LevelheadMainGUI) {
            GlStateManager.scale(0.5, 0.5, 0.0);
        }
        GlStateManager.disableTexture2D();
        worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-stringWidth - 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-stringWidth - 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(stringWidth + 1, 8, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(stringWidth + 1, -1, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        renderString(fontrenderer, tag);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void renderString(FontRenderer renderer, LevelheadTag tag) {
        int x = -renderer.getStringWidth(tag.getString()) >> 1;
        //Render header
        LevelheadComponent header = tag.getHeader();
        render(renderer, header, x);
        x += renderer.getStringWidth(header.getValue());
        //render footer
        render(renderer, tag.getFooter(), x);

    }

    private void render(FontRenderer renderer, LevelheadComponent header, int x) {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);

        if (header.isRgb()) {
            renderer.drawString(header.getValue(), x, 0, new Color((float) header.getRed() / 255F,
                (float) header.getGreen() / 255F,
                (float) header.getBlue() / 255F,
                .2F).getRGB());
        } else if (header.isChroma()) {
            renderer.drawString(header.getValue(), x, 0, Levelhead.getRGBDarkColor());
        } else {
            GlStateManager.color(255, 255, 255, .5F);
            renderer.drawString(header.getColor() + header.getValue(), x, 0, 0x2affffd6);
        }

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        GlStateManager.color(1.0F, 1.0F, 1.0F);
        if (header.isRgb()) {
            GlStateManager.color(header.getRed(), header.getBlue(), header.getGreen(), header.getAlpha());
            renderer.drawString(header.getValue(), x, 0, new Color(header.getRed(), header.getGreen(), header.getBlue()).getRGB());
        } else if (header.isChroma()) {
            renderer.drawString(header.getValue(), x, 0, Levelhead.getRGBColor());
        } else {
            GlStateManager.color(255, 255, 255, .5F);
            renderer.drawString(header.getColor() + header.getValue(), x, 0, 0xffb2b2b2);
        }
    }
}
