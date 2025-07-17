package club.sk1er.mods.levelhead.auth

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.gson
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.Levelhead.logger
import club.sk1er.mods.levelhead.Levelhead.okHttpClient
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException

class MojangAuth {
    var accessKey: String = ""
        private set
    var isFailed = false
        private set
    var failMessage: String = ""
        private set
    var hash: String = ""
        private set

    private fun fail(message: String) {
        failMessage = message
        isFailed = true
    }

    fun auth() {
        val uuid = Minecraft.getMinecraft().session.profile.id
        val jsonObject = jsonParser.parse(Levelhead.getWithAgent(
            "https://api.sk1er.club/auth/begin?uuid=$uuid&mod=${Levelhead.MODID}&ver=${Levelhead.VERSION}"
        )).asJsonObject
        if (!jsonObject["success"].asBoolean) {
            fail("Error during init: $jsonObject")
            return
        }

        hash = jsonObject["hash"].asString

        val session = Minecraft.getMinecraft().session.token
        logger.debug("Logging in with details: Server-Hash: {}, Session: {}, UUID={}", hash, session, uuid)
        val statusCode = joinServer(session, uuid.toString().replace("-", ""), hash)
        if (statusCode != 204) {
            fail("Error during Mojang Auth (1) $statusCode")
            return
        }

        val finalResponse = jsonParser.parse(Levelhead.getWithAgent(
            "https://api.sk1er.club/auth/final?hash=" + hash + "&name=" + Minecraft.getMinecraft().session.profile.name
        )).asJsonObject
        logger.debug("Final auth response: {}", finalResponse)
        if (finalResponse["success"].asBoolean) {
            accessKey = finalResponse["access_key"].asString
            logger.debug("Successfully authenticated with Levelhead")
        } else {
            fail("Error during final auth. Reason: " + finalResponse["cause"].asString)
        }
    }

    private fun joinServer(token: String, uuid: String, serverHash: String): Int {
        val json = JsonObject().also { json ->
            mapOf(
                "accessToken" to token,
                "selectedProfile" to uuid,
                "serverId" to serverHash
            ).forEach { (key, value) ->
                json.addProperty(key, value)
            }
        }
        val body: RequestBody = RequestBody.create(
            MediaType.parse("application/json"), gson.toJson(json))
        val request = Request.Builder()
            .url("https://sessionserver.mojang.com/session/minecraft/join")
            .post(body)
            .build()
        val response = okHttpClient.newCall(request).execute()
        return try {
            response.code()
        } catch (e: IOException) {
            -1
        } finally {
            response.close()
        }
    }
}