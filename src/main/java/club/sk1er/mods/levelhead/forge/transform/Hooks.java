package club.sk1er.mods.levelhead.forge.transform;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.display.DisplayConfig;
import club.sk1er.mods.levelhead.display.LevelheadDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

import java.awt.Color;

@SuppressWarnings("unused")
public final class Hooks {

    public static void drawPingHook(int i, int x, int y, NetworkPlayerInfo playerInfo) {
        if (!Levelhead.INSTANCE.getDisplayManager().getMasterConfig().isEnabled()) {
            return;
        }
        Levelhead instance = Levelhead.INSTANCE;
        LevelheadDisplay tab = instance.getDisplayManager().getTab();
        if (tab != null) {
            if (!tab.getConfig().isEnabled()) {
                return;
            }

            if (instance.getLevelheadPurchaseStates().isTab()) {
                if (!tab.getConfig().isShowSelf() && playerInfo.getGameProfile().getId().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) return;
                String s = tab.getTrueValueCache().get(playerInfo.getGameProfile().getId());
                if (s != null) {
                    FontRenderer fontRendererObj = Minecraft.getMinecraft().fontRendererObj;
                    int x1 = i + x - 12 - fontRendererObj.getStringWidth(s);

                    Scoreboard board = Minecraft.getMinecraft().theWorld.getScoreboard();
                    ScoreObjective objective = board.getObjectiveInDisplaySlot(0);

                    if (objective != null) {
                        int score = board.getValueFromObjective(playerInfo.getGameProfile().getName(), objective).getScorePoints();
                        int extraWidth = fontRendererObj.getStringWidth(" " + score);

                        x1 -= extraWidth;
                    }

                    DisplayConfig config = tab.getConfig();
                    if (config.isHeaderChroma()) {
                        fontRendererObj.drawString(s, x1, y, Levelhead.getRGBColor());
                    } else if (config.isHeaderRgb()) {
                        fontRendererObj.drawString(s, x1, y, new Color(config.getHeaderRed(), config.getHeaderGreen(), config.getHeaderBlue()).getRGB());
                    } else {
                        fontRendererObj.drawString(config.getFooterColor() + s, x1, y, Color.WHITE.getRGB());
                    }
                }
            }
        }
    }


    public static int getLevelheadWidth(NetworkPlayerInfo playerInfo) {
        if (!Levelhead.INSTANCE.getDisplayManager().getMasterConfig().isEnabled()) {
            return 0;
        }
        Levelhead instance = Levelhead.INSTANCE;
        LevelheadDisplay tab = instance.getDisplayManager().getTab();
        if (tab != null) {
            if (!tab.getConfig().isEnabled())
                return 0;
            if (instance.getLevelheadPurchaseStates().isTab()) {
                String s = tab.getTrueValueCache().get(playerInfo.getGameProfile().getId());
                if (s != null) {
                    return Minecraft.getMinecraft().fontRendererObj.getStringWidth(s) + 2;
                }
            }
        }
        return 0;
    }

}
