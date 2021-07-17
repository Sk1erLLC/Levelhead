package club.sk1er.mods.levelhead.auth;

import club.sk1er.mods.levelhead.Levelhead;
import gg.essential.api.utils.JsonHolder;
import gg.essential.api.utils.WebUtil;
import me.semx11.autotip.util.LoginUtil;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class MojangAuth {

    private String accessKey;

    private boolean failed = false;
    private String failMessage = null;
    private String hash;

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
        final Logger logger = Levelhead.INSTANCE.getLogger();
        logger.debug("Logging in with details: Server-Hash: {}, Session: {}, UUID={}", hash, session, uuid);

        int statusCode = LoginUtil.joinServer(session, uuid.toString().replace("-", ""), hash);
        if (statusCode != 204) {
            fail("Error during Mojang Auth (1) " + statusCode);
            return;
        }

        JsonHolder finalResponse = WebUtil.fetchJSON("https://api.sk1er.club/auth/final?hash=" + hash + "&name=" + Minecraft.getMinecraft().getSession().getProfile().getName());
        logger.debug("Final auth response: {}", finalResponse);
        if (finalResponse.optBoolean("success")) {
            this.accessKey = finalResponse.optString("access_key");
            logger.debug("Successfully authenticated with Levelhead");
        } else {
            fail("Error during final auth. Reason: " + finalResponse.optString("cause"));
        }
    }
}
