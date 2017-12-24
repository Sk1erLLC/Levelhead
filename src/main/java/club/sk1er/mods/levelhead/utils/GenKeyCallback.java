package club.sk1er.mods.levelhead.utils;

import com.google.gson.JsonObject;

/**
 * Created by mitchellkatz on 12/21/17. Designed for production use on Sk1er.club
 */
public interface GenKeyCallback {
    void call(JsonObject object);
}
