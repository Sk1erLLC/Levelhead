package club.sk1er.mods.levelhead.renderer;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.AboveHeadDisplay;
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
    public void render(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        if (levelhead == null
            || levelhead.getDisplayManager() == null
            || levelhead.getDisplayManager().getMasterConfig() == null
            || !levelhead.getDisplayManager().getMasterConfig().isEnabled()) {
            return;
        }

        //#if MC<=10809
        if (!(event.entity instanceof EntityPlayer)) return;
        //#else
        //$$ if (!(event.getEntity() instanceof EntityPlayer)) return;
        //#endif

        //#if MC<=10809
        EntityPlayer player = (EntityPlayer) event.entity;
        //#else
        //$$ EntityPlayer player = (EntityPlayer) event.getEntity();
        //#endif
        int o = 0;
        for (AboveHeadDisplay display : levelhead.getDisplayManager().getAboveHead()) {
            int index = display.getIndex();
            int extraHead = levelhead.getLevelheadPurchaseStates().getExtraHead();
            if (index > extraHead || !display.getConfig().isEnabled()) continue;
            LevelheadTag levelheadTag = display.getCache().get(player.getUniqueID());

            if (display.loadOrRender(player) && levelheadTag != null && !(levelheadTag instanceof NullLevelheadTag)) {
                if ((player.getUniqueID().equals(Levelhead.getInstance().userUuid) && !display.getConfig().isShowSelf()) || !EssentialAPI.getMinecraftUtil().isHypixel())
                    continue;

                if (player.getDistanceSqToEntity(UMinecraft.getPlayer()) < 4096) {
                    double offset = 0.3;
                    Scoreboard scoreboard = player.getWorldScoreboard();
                    ScoreObjective scoreObjective = scoreboard.getObjectiveInDisplaySlot(2);

                    if ((scoreObjective != null) && (player.getDistanceSqToEntity(UMinecraft.getPlayer()) < 100)) {
                        offset *= 2;
                    }
                    if (player.getUniqueID().equals(Levelhead.getInstance().userUuid)) offset = 0;
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
        float f = (float) (1.6F * Levelhead.getInstance().getDisplayManager().getMasterConfig().getFontSize());
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();

        int xMultiplier = 1;
        final Minecraft mc = UMinecraft.getMinecraft();
        if (mc.gameSettings != null && mc.gameSettings.thirdPersonView == 2) {
            xMultiplier = -1;
        }

        GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        final RenderManager renderManager = mc.getRenderManager();
        GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(renderManager.playerViewX * xMultiplier, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int stringWidth = fontrenderer.getStringWidth(tag.getString()) >> 1;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
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
