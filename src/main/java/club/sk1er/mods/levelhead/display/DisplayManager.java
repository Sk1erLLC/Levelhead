package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.core.util.JsonHolder;
import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.levelhead.config.MasterConfig;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayManager {

    private final Gson GSON = new Gson();
    private final List<AboveHeadDisplay> aboveHead = new ArrayList<>();
    private LevelheadDisplay chat = null;
    private TabDisplay tab = null;
    private MasterConfig config = new MasterConfig();
    private final File file;

    public DisplayManager(JsonHolder source, File file) {
        if (source == null)
            source = new JsonHolder();
        this.file = file;
        if (source.has("master")) {
            try {
                this.config = GSON.fromJson(source.optJSONObject("master").getObject(), MasterConfig.class);
            } catch (Exception ignored) {

            }
        }
        if (config == null) {
            this.config = new MasterConfig();
            MinecraftUtils.sendMessage("Could not load previous settings! If this is your first time running the mod, nothing is wrong.˚");
        }
        for (JsonElement head : source.optJSONArray("head")) {
            try {
                final JsonObject asJsonObject = head.getAsJsonObject();
                if (asJsonObject.has("type") && asJsonObject.get("type").getAsString().equalsIgnoreCase("SONG"))
                    aboveHead.add(new SongDisplay(GSON.fromJson(asJsonObject, DisplayConfig.class)));
                else
                    aboveHead.add(new AboveHeadDisplay(GSON.fromJson(asJsonObject, DisplayConfig.class)));
            } catch (Exception ignored) {

            }
        }
        if (source.has("chat")) {
            try {
                this.chat = new ChatDisplay(GSON.fromJson(source.optJSONObject("chat").getObject(), DisplayConfig.class));
            } catch (Exception ignored) {

            }
        }
        if (source.has("tab")) {
            try {
                this.tab = new TabDisplay(GSON.fromJson(source.optJSONObject("tab").getObject(), DisplayConfig.class));
            } catch (Exception ignored) {

            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::save));

        if (aboveHead.isEmpty()) {
            aboveHead.add(new AboveHeadDisplay(new DisplayConfig()));
        }

        boolean song = false;
        for (AboveHeadDisplay aboveHeadDisplay : aboveHead) {
            if (aboveHeadDisplay.getConfig().getType().equalsIgnoreCase("SONG")) {
                song = true;
                break;
            }
        }

        if (!song) {
            aboveHead.add(new SongDisplay(new DisplayConfig()));
        }
        if (tab == null) {
            DisplayConfig config = new DisplayConfig();
            config.setType("QUESTS");
            tab = new TabDisplay(config);
        }

        adjustIndexes();

        if (chat == null) {
            DisplayConfig config = new DisplayConfig();
            config.setType("GUILD_NAME");
            chat = new ChatDisplay(config);
        }


    }

    public void adjustIndexes() {
        aboveHead.sort((o1, o2) -> o1 instanceof SongDisplay && o2 instanceof SongDisplay ? 0 : (
            o1 instanceof SongDisplay ? 1 : (
                o2 instanceof SongDisplay ? -1 : 1
            )
        ));
        for (int i = 0; i < aboveHead.size(); i++) {
            aboveHead.get(i).setBottomValue(i == 0);
            aboveHead.get(i).setIndex(i);
        }
    }

    public List<AboveHeadDisplay> getAboveHead() {
        return aboveHead;
    }

    public LevelheadDisplay getChat() {
        return chat;
    }

    public LevelheadDisplay getTab() {
        return tab;
    }

    public MasterConfig getMasterConfig() {
        return config;
    }

    public void tick() {
        if (!config.isEnabled()) {
            return;
        }

        for (AboveHeadDisplay aboveHeadDisplay : aboveHead) {
            aboveHeadDisplay.tick();
        }

        if (tab != null)
            tab.tick();
        if (chat != null)
            chat.tick();
    }

    public void checkCacheSizes() {
        for (AboveHeadDisplay aboveHeadDisplay : aboveHead) {
            aboveHeadDisplay.checkCacheSize();
        }

        if (tab != null) {
            tab.checkCacheSize();
        }
        if (chat != null) {
            chat.checkCacheSize();
        }
    }

    public void save() {
        JsonHolder jsonHolder = new JsonHolder();
        jsonHolder.put("master", new JsonHolder(GSON.toJson(config)));
        if (tab != null) {
            jsonHolder.put("tab", new JsonHolder(GSON.toJson(tab.getConfig())));
        }
        if (chat != null) {
            jsonHolder.put("chat", new JsonHolder(GSON.toJson(chat.getConfig())));
        }
        JsonArray head = new JsonArray();
        for (AboveHeadDisplay aboveHeadDisplay : this.aboveHead) {
            head.add(new JsonHolder(GSON.toJson(aboveHeadDisplay.getConfig())).getObject());
        }
        jsonHolder.put("head", head);
        try {
            FileUtils.writeStringToFile(this.file, jsonHolder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void clearCache() {
        for (AboveHeadDisplay aboveHeadDisplay : this.aboveHead) {
            aboveHeadDisplay.cache.clear();
            aboveHeadDisplay.trueValueCache.clear();
        }

        if (tab != null) {
            tab.cache.clear();
            tab.trueValueCache.clear();
        }

        if (chat != null) {
            chat.cache.clear();
            chat.trueValueCache.clear();
        }
    }
}
