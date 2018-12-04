package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.config.MasterConfig;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class DisplayManager {

    private Gson GSON = new Gson();
    private List<LevelheadDisplay> aboveHeaad = new ArrayList<>();
    private LevelheadDisplay chat = null;
    private LevelheadDisplay tab = null;
    private MasterConfig config = new MasterConfig();

    public DisplayManager(JsonHolder source) {
        if (source.has("master")) {
            this.config = GSON.fromJson(source.optJsonObject("master").getObject(), MasterConfig.class);
        }

    }

    public List<LevelheadDisplay> getAboveHeaad() {
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
