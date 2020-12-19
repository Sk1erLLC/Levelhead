package club.sk1er.mods.levelhead.auth;

import club.sk1er.mods.levelhead.Levelhead;
import me.semx11.autotip.util.LoginUtil;
import net.minecraft.client.Minecraft;
import net.modcore.api.utils.JsonHolder;
import net.modcore.api.utils.WebUtil;

import java.util.UUID;

public class MojangAuth {

    private String accessKey;

    private boolean failed = false;
    private String failMessage = null;
    private boolean success = false;
    private String hash;

    public MojangAuth() {

    }

    public String getAccessKey() {
        return accessKey;
    }

    public boolean isFailed() {
        return failed;
    }

    public String getFailMessage() {
        return failMessage;
    }

    public void fail(String message) {
        this.failMessage = message;
        this.failed = true;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getHash() {
        return hash;
    }

    public void auth() {

        UUID uuid = Minecraft.getMinecraft().getSession().getProfile().getId();
        JsonHolder jsonHolder = WebUtil.fetchJSON("https://api.sk1er.club/auth/begin?uuid=" + uuid + "&mod=" + Levelhead.MODID + "&ver=" + Levelhead.VERSION);
        if (!jsonHolder.optBoolean("success")) {
            fail("Error during init: " + jsonHolder);
            return;
        }

        hash = jsonHolder.optString("hash");

        String session = Minecraft.getMinecraft().getSession().getToken();
        System.out.println("Logging in with details: Server-Hash: " + hash + " Session: " + session + " UUID=" + uuid);

        int statusCode = LoginUtil.joinServer(session, uuid.toString().replace("-", ""), hash);
        if (statusCode != 204) {
            fail("Error during Mojang Auth (1) " + statusCode);
            return;
        }

        JsonHolder finalResponse = WebUtil.fetchJSON("https://api.sk1er.club/auth/final?hash=" + hash + "&name=" + Minecraft.getMinecraft().getSession().getProfile().getName());
        System.out.println("FINAL RESPONSE: " + finalResponse);
        if (finalResponse.optBoolean("success")) {
            this.accessKey = finalResponse.optString("access_key");
            this.success = true;
            System.out.println("Successfully authenticated with Sk1er.club Levelhead");
        } else {
            fail("Error during final auth. Reason: " + finalResponse.optString("cause"));
        }

    }


}
