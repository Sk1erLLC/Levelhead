package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.config.MasterConfig;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DisplayManager {

    private Gson GSON = new Gson();
    private List<AboveHeadDisplay> aboveHead = new ArrayList<>();
    private LevelheadDisplay chat = null;
    private TabDisplay tab = null;
    private MasterConfig config = new MasterConfig();
    private File file;

    public DisplayManager(JsonHolder source, File file) {
        this.file = file;
        if (source.has("master")) {
            this.config = GSON.fromJson(source.optJsonObject("master").getObject(), MasterConfig.class);
        } else {
            this.config = new MasterConfig();
        }
        for (JsonElement head : source.optJSONArray("head")) {
            aboveHead.add(new AboveHeadDisplay(GSON.fromJson(head.getAsJsonObject(), DisplayConfig.class)));
        }
        if (source.has("chat")) {
            this.chat = new ChatDisplay(GSON.fromJson(source.optJsonObject("chat").getObject(), DisplayConfig.class));
        }
        if (source.has("tab")) {
            this.tab = new TabDisplay(GSON.fromJson(source.optJsonObject("tab").getObject(), DisplayConfig.class));
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::save));

        if (aboveHead.isEmpty()) {
            aboveHead.add(new AboveHeadDisplay(new DisplayConfig()));
        }
        //TODO remove debug
        if (aboveHead.size() != 2) {
            DisplayConfig config = new DisplayConfig();
            config.setType("KARMA");
            config.setFooterChroma(true);
            aboveHead.add(new AboveHeadDisplay(config));

        }
        if (tab == null) {
            DisplayConfig config = new DisplayConfig();
            config.setType("QUESTS");
            tab = new TabDisplay(config);
        }

        for (int i = 0; i < aboveHead.size(); i++) {
            aboveHead.get(i).setBottomValue(i == 0);
        }

        if(chat == null) {
            DisplayConfig config = new DisplayConfig();
            config.setType("GUILD_NAME");
            chat = new ChatDisplay(config);
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
        aboveHead.forEach(LevelheadDisplay::tick);
        if (tab != null)
            tab.tick();
        if (chat != null)
            chat.tick();
    }

    public void checkCacheSizes() {
        aboveHead.forEach(LevelheadDisplay::checkCacheSize);
        if (tab != null) {
            tab.checkCacheSize();
        }
        if (chat != null) {
            chat.checkCacheSize();
        }
    }

    public void save() {
        JsonHolder jsonHolder = new JsonHolder();
        jsonHolder.put("master", new JsonHolder(GSON.toJson(getMasterConfig())));
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
            chat.cache.clear();
        }
    }
}
