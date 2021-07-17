package me.semx11.autotip.util;

import club.sk1er.mods.levelhead.Levelhead;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LoginUtil {
    public static int joinServer(String token, String uuid, String serverHash) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/join").openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            JsonObject obj = new JsonObject();
            obj.addProperty("accessToken", token);
            obj.addProperty("selectedProfile", uuid);
            obj.addProperty("serverId", serverHash);

            byte[] jsonBytes = obj.toString().getBytes(StandardCharsets.UTF_8);

            connection.setFixedLengthStreamingMode(jsonBytes.length);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.connect();

            try (OutputStream out = connection.getOutputStream()) {
                out.write(jsonBytes);
            }
            int responseCode = connection.getResponseCode();
            Levelhead.INSTANCE.getLogger().debug("Mojang response code: {}", responseCode);
            return responseCode;
        } catch (IOException e) {
            return -1;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
