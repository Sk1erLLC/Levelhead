package club.sk1er.mods.levelhead

import club.sk1er.mods.levelhead.auth.MojangAuth
import club.sk1er.mods.levelhead.commands.LevelheadCommand
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.DisplayManager
import club.sk1er.mods.levelhead.core.RateLimiter
import club.sk1er.mods.levelhead.core.trimmed
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import club.sk1er.mods.levelhead.display.LevelheadTag
import club.sk1er.mods.levelhead.render.AboveHeadRender
import club.sk1er.mods.levelhead.render.ChatRender
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import gg.essential.api.EssentialAPI
import gg.essential.api.utils.Multithreading
import gg.essential.universal.ChatColor
import gg.essential.universal.UMinecraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.Color
import java.io.File
import java.text.DecimalFormat
import java.time.Duration
import java.util.*

/**
 * TODO
 * Adapt Sk1er.club API to new config style
 */
@Mod(modid = Levelhead.MODID, name = "Levelhead", version = Levelhead.VERSION, modLanguageAdapter = "gg.essential.api.utils.KotlinAdapter")
object Levelhead {
    val logger: Logger = LogManager.getLogger()
    val okHttpClient = OkHttpClient()
    val gson = Gson()
    val jsonParser = JsonParser()

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
    private val rateLimiter: RateLimiter = RateLimiter(100, Duration.ofSeconds(1))
    private val format: DecimalFormat = DecimalFormat("#,###")
    val DarkChromaColor: Int
        get() = Color.HSBtoRGB(System.currentTimeMillis() % 1000 / 1000f, 0.8f, 0.2f)
    val ChromaColor: Int
        get() = Color.HSBtoRGB(System.currentTimeMillis() % 1000 / 1000f, 0.8f, 0.8f)

    const val MODID = "level_head"
    const val VERSION = "8.0.0"

    @Mod.EventHandler
    fun preInit(event: FMLPreInitializationEvent) {
        Multithreading.runAsync {
            types = jsonParser.parse(rawWithAgent("https://api.sk1er.club/levelhead_config")).asJsonObject
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
        rawPurchases = jsonParser.parse(rawWithAgent(
            "https://api.sk1er.club/purchases/" + UMinecraft.getMinecraft().session.profile.id.toString()
        )).asJsonObject
        if (!rawPurchases.has("remaining_levelhead_credits")) {
            rawPurchases.addProperty("remaining_levelhead_credits", 0)
        }
    }

    @Synchronized
    fun refreshPaidData() {
        paidData = jsonParser.parse(rawWithAgent("https://api.sk1er.club/levelhead_data")).asJsonObject
    }

    @Synchronized
    fun refreshPurchaseStates() {
        purchaseStatus = jsonParser.parse(rawWithAgent(
            "https://api.sk1er.club/levelhead_purchase_status/" + UMinecraft.getMinecraft().session.profile.id.toString()
        )).asJsonObject
        LevelheadPurchaseStates.chat = purchaseStatus["chat"].asBoolean
        LevelheadPurchaseStates.tab = purchaseStatus["tab"].asBoolean
        LevelheadPurchaseStates.aboveHead = purchaseStatus["head"].asInt
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
            displayManager.joinWorld()
        // when others join world
        } else if (event.entity is EntityPlayer) {
            displayManager.playerJoin(event.entity as EntityPlayer)
        }
    }

    fun fetch(uuid: UUID, display: LevelheadDisplay, allowOverride: Boolean) {
        val type = display.config.type

        scope.launch {
            rateLimiter.consume()


            val url = "https://api.sk1er.club/levelheadv8/${uuid.trimmed}/" +
                "$type/${UMinecraft.getMinecraft().session.profile.id.trimmed}/" +
                "$VERSION/${auth.hash}/${display.displayPosition.name}"
            val res = jsonParser.parse(rawWithAgent(url)).asJsonObject

            if (!res["success"].asBoolean) {
                res.addProperty("footerString", "Error")
            }

            if (!allowOverride) {
                res.remove("header_obj")
                res.remove("footer_obj")
            }

            val tag = buildTag(res, uuid, display, allowOverride)
            display.cache[uuid] = tag
        }
    }

    private suspend fun buildTag(jsonObject: JsonObject, uuid: UUID, display: LevelheadDisplay, allowOverride: Boolean): LevelheadTag {
        val value = LevelheadTag(uuid)

        val headerObj = JsonObject()
        val footerObj = JsonObject()
        val construct = JsonObject()

        jsonObject["headerColor"]?.run {
            if (!allowOverride) return@run
            headerObj.add("chroma", jsonObject["headerChroma"])
            headerObj.add("color", this)
            headerObj.addProperty("custom", true)
        }

        jsonObject["footerColor"]?.run {
            if (!allowOverride) return@run
            footerObj.add("chroma", jsonObject["footerChroma"])
            footerObj.add("color", this)
            footerObj.addProperty("custom", true)
        }

        if (jsonObject.has("headerString") && allowOverride) {
            headerObj.addProperty("string", jsonObject["headerString"].asString)
            headerObj.addProperty("custom", true)
        }

        if (jsonObject.has("footerString") && allowOverride) {
            footerObj.addProperty("string", jsonObject["footerString"].asString)
            footerObj.addProperty("custom", true)
        } else {
            footerObj.addProperty("string", jsonObject["value"].asString)
            footerObj.addProperty("custom", false)
        }

        headerObj.merge(display.headerConfig, !allowOverride)
        footerObj.merge(display.footerConfig, !allowOverride)

        construct.addProperty("exclude", jsonObject["exclude"]?.asBoolean ?: false)
        construct.add("header", headerObj)
        construct.add("footer", footerObj)

        value.construct(construct)
        return value
    }

    fun rawWithAgent(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V${VERSION})")
            .get()
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

    object LevelheadPurchaseStates {
        var chat: Boolean = false
        var tab: Boolean = false
        var aboveHead: Int = 1
    }
}