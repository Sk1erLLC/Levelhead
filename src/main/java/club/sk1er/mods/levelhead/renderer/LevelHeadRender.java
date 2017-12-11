package club.sk1er.mods.levelhead.renderer;

import club.sk1er.mods.levelhead.LevelHead;
import club.sk1er.mods.levelhead.utils.Sk1erMod;

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

import java.awt.*;

/**
 * Created by mitchellkatz
 *
 * Modified by boomboompower on 16/6/2017
 */
public class LevelHeadRender {

    private LevelHead levelHead;

    public LevelHeadRender(LevelHead levelHead) {
        this.levelHead = levelHead;
    }

    @SubscribeEvent
    public void render(RenderPlayerEvent.Pre event) {
        if (event.entityPlayer.getUniqueID().equals(LevelHead.UUID) || !Sk1erMod.getInstance().isHypixel()) return;

        EntityPlayer player = event.entityPlayer;
        // TODO - "entityPlayer" field is private for newer versions (PORTING REQUIRED) - Newer versions use a getter instead
        // TODO - https://github.com/MinecraftForge/MinecraftForge/blob/1.11.x/src/main/java/net/minecraftforge/event/entity/player/PlayerEvent.java#L51-L54

        if (levelHead.loadOrRender(player) && (LevelHead.getInstance().getLevelString(player.getUniqueID())) != null) {
            if (player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) < 64 * 64) {
                double offset = 0.3;
                Scoreboard scoreboard = player.getWorldScoreboard();
                ScoreObjective scoreObjective = scoreboard.getObjectiveInDisplaySlot(2);

                if (scoreObjective != null) {
                    offset *= 2;
                }
                renderName(event, (LevelHead.getInstance().getLevelString(player.getUniqueID())), player, event.x, event.y + offset, event.z);
            }
        }
    }

    public void renderName(RenderPlayerEvent event, String str, EntityPlayer entityIn, double x, double y, double z) {
        if (!LevelHead.TOGGLED_ON || str.isEmpty())
            return;
        FontRenderer fontrenderer = event.renderer.getFontRendererFromRenderManager();
        float f = 1.6F;
        float f1 = 0.016666668F * f;
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x + 0.0F, (float) y + entityIn.height + 0.5F, (float) z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-event.renderer.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(event.renderer.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-f1, -f1, f1);
        GlStateManager.disableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        int i = 0;

        int j = fontrenderer.getStringWidth(str) / 2;
        GlStateManager.disableTexture2D();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos((double) (-j - 1), (double) (-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (-j - 1), (double) (8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (j + 1), (double) (8 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        worldrenderer.pos((double) (j + 1), (double) (-1 + i), 0.0D).color(0.0F, 0.0F, 0.0F, 0.25F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();

        renderString(fontrenderer, str, i);

        GlStateManager.enableLighting();
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void renderString(FontRenderer renderer, String str, int y) {
        String[] sp = str.split(":"); // Level: 0

        String left = sp[0] + ":"; // Level:
        String right = sp[1]; //0

        int x = -renderer.getStringWidth(str) / 2;

        renderer.drawString(LevelHead.PRIMARY_CHROMA ? left : LevelHead.PRIMARY_COLOR + left, x, y, LevelHead.PRIMARY_CHROMA ? LevelHead.getColorDark() :553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        renderer.drawString(LevelHead.PRIMARY_CHROMA ? left : LevelHead.PRIMARY_COLOR + left, x, y, LevelHead.PRIMARY_CHROMA ? LevelHead.getColor() : Color.WHITE.getRGB());

        renderer.drawString(LevelHead.SECONDARY_CHROMA ? right : LevelHead.SECOND_COLOR + right, x + renderer.getStringWidth(left), y, LevelHead.SECONDARY_CHROMA ? LevelHead.getColorDark() : 553648127);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        renderer.drawString(LevelHead.SECONDARY_CHROMA ? right : LevelHead.SECOND_COLOR + right, x + renderer.getStringWidth(left), y, LevelHead.SECONDARY_CHROMA ? LevelHead.getColor() : Color.WHITE.getRGB());
    }
}
