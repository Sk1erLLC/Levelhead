package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import club.sk1er.mods.levelhead.renderer.LevelheadTag;
import gg.essential.api.utils.JsonHolder;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LevelheadDisplay {
    protected final ConcurrentHashMap<UUID, LevelheadTag> cache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<UUID, String> trueValueCache = new ConcurrentHashMap<>();
    protected final List<UUID> existedMorethan5Seconds = new ArrayList<>();
    protected final Map<UUID, Integer> timeCheck = new HashMap<>();

    private final DisplayPosition position;
    private final DisplayConfig config;

    public LevelheadDisplay(DisplayPosition position, DisplayConfig config) {
        this.position = position;
        this.config = config;
    }

    public DisplayConfig getConfig() {
        return config;
    }

    public DisplayPosition getPosition() {
        return position;
    }

    public JsonHolder getHeaderConfig() {
        return new JsonHolder()
            .put("chroma", config.isHeaderChroma())
            .put("rgb", config.isHeaderRgb())
            .put("red", config.getHeaderRed())
            .put("green", config.getHeaderGreen())
            .put("blue", config.getHeaderBlue())
            .put("color", config.getHeaderColor())
            .put("alpha", config.getHeaderAlpha())
            .put("header", config.getCustomHeader() + ": ");
    }

    public JsonHolder getFooterConfig() {
        return new JsonHolder()
            .put("chroma", config.isFooterChroma())
            .put("rgb", config.isFooterRgb())
            .put("color", config.getFooterColor())
            .put("red", config.getFooterRed())
            .put("green", config.getFooterGreen())
            .put("blue", config.getFooterBlue())
            .put("alpha", config.getFooterAlpha());
    }

    public abstract void tick();

    public abstract void checkCacheSize();


    public abstract void onDelete();

    public boolean loadOrRender(EntityPlayer player) {
        return !player.getDisplayName().getFormattedText().contains(LevelheadMainGUI.COLOR_CHAR + "k");
    }

    public ConcurrentHashMap<UUID, LevelheadTag> getCache() {
        return cache;
    }

    public ConcurrentHashMap<UUID, String> getTrueValueCache() {
        return trueValueCache;
    }

    public List<UUID> getExistedMorethan5Seconds() {
        return existedMorethan5Seconds;
    }

    public Map<UUID, Integer> getTimeCheck() {
        return timeCheck;
    }

}
