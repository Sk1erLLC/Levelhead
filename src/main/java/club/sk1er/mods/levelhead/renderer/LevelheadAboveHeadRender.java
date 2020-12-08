package club.sk1er.mods.levelhead.renderer;

import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.AboveHeadDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Created by mitchellkatz
 * <p>
 * Modified by boomboompower on 16/6/2017
 */
public class LevelheadAboveHeadRender {

    private final Levelhead levelhead;

    public LevelheadAboveHeadRender(Levelhead levelhead) {
        this.levelhead = levelhead;
    }

    @SubscribeEvent
    public void render(RenderPlayerEvent.Pre event) {
        if (levelhead == null
            || levelhead.getDisplayManager() == null
            || levelhead.getDisplayManager().getMasterConfig() == null
            || !levelhead.getDisplayManager().getMasterConfig().isEnabled()) {
            return;
        }
        //#if MC<=10809
        EntityPlayer player = event.entityPlayer;
        //#else
        //$$ EntityPlayer player = event.getEntityPlayer();
        //#endif
        int o = 0;
        for (AboveHeadDisplay display : levelhead.getDisplayManager().getAboveHead()) {
            int index = display.getIndex();
            int extraHead = levelhead.getLevelheadPurchaseStates().getExtraHead();
            if (index > extraHead || !display.getConfig().isEnabled()) {
                continue;
            }
            LevelheadTag levelheadTag = display.getCache().get(player.getUniqueID());

            if (display.loadOrRender(player) && levelheadTag != null && !(levelheadTag instanceof NullLevelheadTag)) {
                if ((player.getUniqueID().equals(Levelhead.getInstance().userUuid) && !display.getConfig().isShowSelf()) || !MinecraftUtils.isHypixel())
                    continue;

                if (player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) < 64 * 64) {
                    double offset = 0.3;
                    Scoreboard scoreboard = player.getWorldScoreboard();
                    ScoreObjective scoreObjective = scoreboard.getObjectiveInDisplaySlot(2);

                    if (scoreObjective != null && player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) < 10 * 10) {
                        offset *= 2;
                    }
                    if (player.getUniqueID().equals(Levelhead.getInstance().userUuid))
                        offset = 0;
                    offset += levelhead.getDisplayManager().getMasterConfig().getOffset();
                    //#if MC<=10809
                    renderName(event, levelheadTag, player, event.x, event.y + offset + o * .3D, event.z);
                    //#else
                    //$$ renderName(event, levelheadTag, player, event.getX(), event.getY() + offset + o * .3D, event.getZ());
                    //#endif
                }
            }
            o++;
        }

    }

    public void renderName(RenderPlayerEvent event, LevelheadTag tag, EntityPlayer entityIn, double x, double y, double z) {
        //#if MC<=10809
        FontRenderer fontrenderer = event.renderer.getFontRendererFromRenderManager();
        //#else
        //$$ FontRenderer fontrenderer = event.getRenderer().getFontRendererFromRenderManager();
        //#endif
        float f = (float) (1.6F * Levelhead.getInstance().getDisplayManager().getMasterConfig().getFontSize());
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();

        int xMultiplier = 1;
        if (Minecraft.getMinecraft() != null && Minecraft.getMinecraft().gameSettings != null && Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            xMultiplier = -1;
        }

        GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        //#if MC<=10809
        GlStateManager.rotate(-event.renderer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(event.renderer.getRenderManager().playerViewX * xMultiplier, 1.0F, 0.0F, 0.0F);
        //#else
        //$$ GlStateManager.rotate(-event.getRenderer().getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        //$$ GlStateManager.rotate(event.getRenderer().getRenderManager().playerViewX * xMultiplier, 1.0F, 0.0F, 0.0F);
        //#endif
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(tag.getString()) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(-j - 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(-j - 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, 8 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos(j + 1, -1 + i, 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        renderString(fontrenderer, tag);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void renderString(FontRenderer renderer, LevelheadTag tag) {
        int x = -renderer.getStringWidth(tag.getString()) / 2;
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

        int y = 0;
        if (header.isRgb()) {
            renderer.drawString(header.getValue(), x, y, new Color((float) header.getRed() / 255F, (float) header.getGreen() / 255F, (float) header.getBlue() / 255F, .2F).getRGB());
        } else if (header.isChroma()) {
            renderer.drawString(header.getValue(), x, y, Levelhead.getRGBDarkColor());
        } else {
            GlStateManager.color(255, 255, 255, .5F);
            renderer.drawString(header.getColor() + header.getValue(), x, y, Color.WHITE.darker().darker().darker().darker().darker().getRGB() * 255);
        }
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);

        GlStateManager.color(1.0F, 1.0F, 1.0F);
        if (header.isRgb()) {
            GlStateManager.color(header.getRed(), header.getBlue(), header.getGreen(), header.getAlpha());
            renderer.drawString(header.getValue(), x, y, new Color(header.getRed(), header.getGreen(), header.getBlue()).getRGB());
        } else if (header.isChroma()) {
            renderer.drawString(header.getValue(), x, y, header.isChroma() ? Levelhead.getRGBColor() : 553648127);
        } else {
            GlStateManager.color(255, 255, 255, .5F);

            renderer.drawString(header.getColor() + header.getValue(), x, y, Color.WHITE.darker().getRGB());
        }
    }
}
