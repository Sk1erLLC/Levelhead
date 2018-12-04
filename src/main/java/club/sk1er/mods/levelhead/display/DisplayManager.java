package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.config.MasterConfig;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.List;

public class DisplayManager {

    private Gson GSON = new Gson();
    private List<AboveHeadDisplay> aboveHeaad = new ArrayList<>();
    private LevelheadDisplay chat = null;
    private LevelheadDisplay tab = null;
    private MasterConfig config = new MasterConfig();

    public DisplayManager(JsonHolder source) {
        if (source.has("master")) {
            this.config = GSON.fromJson(source.optJsonObject("master").getObject(), MasterConfig.class);
        }
        for (JsonElement head : source.optJSONArray("head")) {
            aboveHeaad.add(GSON.fromJson(head.getAsJsonObject(), AboveHeadDisplay.class));
        }
        if (source.has("chat")) {
//            this.chat = GSON.fromJson(source.optJsonObject("chat").getObject(), LevelheadDisplay.class); //TODO update to proper class name
        }
        if (source.has("tab")) {
//            this.tab = GSON.fromJson(source.optJsonObject("tab").getObject(), LevelheadDisplay.class); //TODO update to proper class name
        }

    }

    public List<AboveHeadDisplay> getAboveHeaad() {
        return aboveHeaad;
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
        aboveHeaad.forEach(LevelheadDisplay::tick);
        if (tab != null)
            tab.tick();
        if (chat != null)
            chat.tick();
    }

    public void checkCacheSizes() {
    //TODO
    }

    public void save() {
        //TODO
    }
}
