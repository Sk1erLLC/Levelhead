package club.sk1er.mods.levelhead

import club.sk1er.mods.levelhead.auth.MojangAuth
import club.sk1er.mods.levelhead.commands.LevelheadCommand
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.DisplayManager
import club.sk1er.mods.levelhead.core.RateLimiter
import club.sk1er.mods.levelhead.core.dashUUID
import club.sk1er.mods.levelhead.core.trimmed
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import club.sk1er.mods.levelhead.display.LevelheadTag
import club.sk1er.mods.levelhead.render.AboveHeadRender
import club.sk1er.mods.levelhead.render.ChatRender
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.essential.api.EssentialAPI
import gg.essential.api.utils.Multithreading
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import kotlinx.coroutines.*
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.io.File
import java.text.DecimalFormat
import java.time.Duration
import java.util.*

@Mod(modid = Levelhead.MODID, name = "Levelhead", version = Levelhead.VERSION, modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter")
object Levelhead {
    val logger: Logger = LogManager.getLogger()
    val okHttpClient = OkHttpClient()
    val gson = Gson()
    val jsonParser = JsonParser()

    val EMPTY_BODY: RequestBody = RequestBody.create(null, byteArrayOf())

    lateinit var auth: MojangAuth
        private set
    lateinit var types: JsonObject
        private set
    lateinit var rawPurchases: JsonObject
        private set
    lateinit var paidData: JsonObject
        private set
    lateinit var purchaseStatus: JsonObject
        private set
    val allowedTypes: JsonObject
        get() = JsonObject().merge(types, true).also { obj ->
            paidData["stats"].asJsonObject.entrySet().filter {
                purchaseStatus[it.key].asBoolean
            }.map { obj.add(it.key, it.value) }
        }
    val displayManager: DisplayManager = DisplayManager(File(File(UMinecraft.getMinecraft().mcDataDir, "config"), "levelhead.json"))
    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val rateLimiter: RateLimiter = RateLimiter(100, Duration.ofSeconds(1))
    private val format: DecimalFormat = DecimalFormat("#,###")
    val DarkChromaColor: Int
        get() = Color.HSBtoRGB(System.currentTimeMillis() % 1000 / 1000f, 0.8f, 0.2f)
    val ChromaColor: Int
        get() = Color.HSBtoRGB(System.currentTimeMillis() % 1000 / 1000f, 0.8f, 0.8f)
    val chromaColor: Color
        get() = Color(ChromaColor)
    val selfLevelheadTag
        get() = displayManager.aboveHead[0].cache[UPlayer.getUUID()]!!

    const val MODID = "level_head"
    const val VERSION = "8.0.0-RC1"

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        Multithreading.runAsync {
            types = jsonParser.parse(getWithAgent("https://api.sk1er.club/levelhead_config")).asJsonObject
        }
    }

    @Mod.EventHandler
    fun postInit(ignored: FMLPostInitializationEvent) {
        MinecraftForge.EVENT_BUS.register(AboveHeadRender)
        MinecraftForge.EVENT_BUS.register(ChatRender)
        MinecraftForge.EVENT_BUS.register(this)
        EssentialAPI.getCommandRegistry().registerCommand(LevelheadCommand())
    }


    @Synchronized
    fun refreshRawPurchases() {
        rawPurchases = jsonParser.parse(getWithAgent(
            "https://api.sk1er.club/purchases/" + UMinecraft.getMinecraft().session.profile.id.toString()
        )).asJsonObject
        if (!rawPurchases.has("remaining_levelhead_credits")) {
            rawPurchases.addProperty("remaining_levelhead_credits", 0)
        }
    }

    @Synchronized
    fun refreshPaidData() {
        paidData = jsonParser.parse(getWithAgent("https://api.sk1er.club/levelhead_data")).asJsonObject
    }

    @Synchronized
    fun refreshPurchaseStates() {
        purchaseStatus = jsonParser.parse(getWithAgent(
            "https://api.sk1er.club/levelhead_purchase_status/" + UMinecraft.getMinecraft().session.profile.id.toString()
        )).asJsonObject
        LevelheadPurchaseStates.chat = purchaseStatus["chat"].asBoolean
        LevelheadPurchaseStates.tab = purchaseStatus["tab"].asBoolean
        LevelheadPurchaseStates.aboveHead = purchaseStatus["head"].asInt
        LevelheadPurchaseStates.customLevelhead = purchaseStatus["custom_levelhead"].asBoolean
        for (i in displayManager.aboveHead.size..LevelheadPurchaseStates.aboveHead) {
            displayManager.aboveHead.add(AboveHeadDisplay(DisplayConfig()))
        }
        displayManager.adjustIndices()
    }

    @SubscribeEvent
    fun joinServer(event: FMLNetworkEvent.ClientConnectedToServerEvent) {
        auth = MojangAuth()
        auth.auth()
        if (auth.isFailed) {
            EssentialAPI.getNotifications().push("An error occurred while logging logging into Levelhead", auth.failMessage)
        }
        refreshPurchaseStates()
        refreshRawPurchases()
        refreshPaidData()

    }

    @SubscribeEvent
    fun playerJoin(event: EntityJoinWorldEvent) {
        // when you join world
        if (event.entity is EntityPlayerSP) {
            scope.coroutineContext.cancelChildren()
            rateLimiter.resetState()
            displayManager.joinWorld()
        // when others join world
        } else if (event.entity is EntityPlayer) {
            displayManager.playerJoin(event.entity as EntityPlayer)
        }
    }

    fun fetch(requests: List<LevelheadRequest>): Job {
        return scope.launch {

            rateLimiter.consume()

            val reqMap = requests.associateBy { it.display.toString() to it.uuid }
            val url = "https://api.sk1er.club/levelheadv8?auth=${auth.hash}&" +
                    "uuid=${UMinecraft.getMinecraft().session.profile.id.trimmed}"

            val requestObj = JsonObject().also { obj ->
                obj.add("requests", JsonArray().also { arr ->
                    requests.map { arr.add(gson.toJsonTree(it).asJsonObject.apply { this.addProperty("display", it.display.toString()) }) }
                })
            }

            val res = jsonParser.parse(postWithAgent(url, requestObj)).asJsonObject
            if (!res["success"].asBoolean) {
                logger.error("Api broke?", res)
                return@launch
            }

            res["results"].asJsonArray.forEach {
                it.asJsonObject.let { result ->
                    val uuid = result["uuid"].asString.dashUUID!!
                    val req = reqMap[result["display"].asString to result["uuid"].asString]!!
                    val tag = LevelheadTag.build(uuid) {
                        header {
                            value = if (req.allowOverride && result.has("headerString"))
                                    "${result["headerString"].asString}: "
                                else
                                    "${req.display.config.headerString}: "
                            if (req.allowOverride && result.has("headerColor")) {
                                color = Color(result["headerColor"].asInt)
                                chroma = result["headerChroma"].asBoolean
                            } else {
                                color = req.display.config.headerColor
                                chroma = req.display.config.headerChroma
                            }
                        }
                        footer {
                            value = if (req.allowOverride && result.has("footerString") &&result["footerString"].asString != result["value"].asString)
                                result["footerString"].asString
                            else
                                result["value"].asString
                            if (req.allowOverride && result.has("footerColor")) {
                                color = Color(result["footerColor"].asInt)
                                chroma = result["footerChroma"].asBoolean
                            } else {
                                color = req.display.config.footerColor
                                chroma = req.display.config.footerChroma
                            }
                        }
                    }
                    req.display.cache[uuid] = tag
                }
            }
        }
    }

    fun getWithAgent(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${VERSION})")
            .get()
            .build()
        return kotlin.runCatching {
            okHttpClient.newCall(request).execute().body()?.use { it.string() }!!
        }.getOrDefault("{\"success\":false,\"cause\":\"API_DOWN\"}")
    }

    fun postWithAgent(url: String, jsonObject: JsonObject): String {
        val body = RequestBody.create(MediaType.parse("application/json"), gson.toJson(jsonObject))
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${VERSION})")
            .post(body)
            .build()
        return kotlin.runCatching {
            okHttpClient.newCall(request).execute().body()?.use { it.string() }!!
        }.getOrDefault("{\"success\":false,\"cause\":\"API_DOWN\"}")
    }

    fun JsonObject.merge(other: JsonObject, override: Boolean): JsonObject {
        other.entrySet().map { it.key }.filter { key ->
            override || !this.has(key)
        }.map { key ->
            this.add(key, other[key])
        }
        return this
    }

    class LevelheadRequest(val uuid: String, val display: LevelheadDisplay, val allowOverride: Boolean, val type: String = display.config.type)

    object LevelheadPurchaseStates {
        var chat: Boolean = false
        var tab: Boolean = false
        var aboveHead: Int = 1
        var customLevelhead: Boolean = false
    }
}