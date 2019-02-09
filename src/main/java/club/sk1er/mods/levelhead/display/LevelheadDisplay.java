package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import club.sk1er.mods.levelhead.renderer.LevelheadTag;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class LevelheadDisplay {
    protected final ConcurrentHashMap<UUID, LevelheadTag> cache = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<UUID, String> trueValueCache = new ConcurrentHashMap<>();
    protected final java.util.List<UUID> existedMorethan5Seconds = new ArrayList<>();
    protected final HashMap<UUID, Integer> timeCheck = new HashMap<>();

    private DisplayPosition position;
    private DisplayConfig config;

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
        JsonHolder holder = new JsonHolder();
        holder.put("chroma", config.isHeaderChroma());
        holder.put("rgb", config.isHeaderRgb());
        holder.put("red", config.getHeaderRed());
        holder.put("green", config.getHeaderGreen());
        holder.put("blue", config.getHeaderBlue());
        holder.put("color", config.getHeaderColor());
        holder.put("alpha", config.getHeaderAlpha());
        holder.put("header", config.getCustomHeader() + ": ");
        return holder;
    }

    public JsonHolder getFooterConfig() {
        JsonHolder holder = new JsonHolder();
        holder.put("chroma", config.isFooterChroma());
        holder.put("rgb", config.isFooterRgb());
        holder.put("color", config.getFooterColor());
        holder.put("red", config.getFooterRed());
        holder.put("green", config.getFooterGreen());
        holder.put("blue", config.getFooterBlue());
        holder.put("alpha", config.getFooterAlpha());
        return holder;
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

    public HashMap<UUID, Integer> getTimeCheck() {
        return timeCheck;
    }

}
