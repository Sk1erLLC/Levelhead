package club.sk1er.mods.levelhead.core

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.Levelhead.gson
import club.sk1er.mods.levelhead.Levelhead.jsonParser
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.config.MasterConfig
import club.sk1er.mods.levelhead.display.AboveHeadDisplay
import club.sk1er.mods.levelhead.display.ChatDisplay
import club.sk1er.mods.levelhead.display.LevelheadDisplay
import club.sk1er.mods.levelhead.display.TabDisplay
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.entity.player.EntityPlayer
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class DisplayManager(val file: File) {

    var config = MasterConfig()
    val aboveHead: MutableList<AboveHeadDisplay> = ArrayList()
    lateinit var chat: ChatDisplay
        private set
    lateinit var tab: TabDisplay
        private set

    init {
        this.readConfig()
    }

    fun readConfig() {
        try {
            var shouldSaveCopyNow = false
            if (!file.exists()) {
                file.createNewFile()
                shouldSaveCopyNow = true
            }
            val source = jsonParser.parse(FileUtils.readFileToString(file)).runCatching { asJsonObject }.getOrElse { JsonObject() }
            if (source.has("master")) this.config = gson.fromJson(source["master"].asJsonObject, MasterConfig::class.java)

            if (source.has("head")) {
                for (head in source["head"].asJsonArray) {
                    aboveHead.add(AboveHeadDisplay(gson.fromJson(head.asJsonObject, DisplayConfig::class.java)))
                }
            }

            if (aboveHead.isEmpty()) {
                aboveHead.add(AboveHeadDisplay(DisplayConfig()))
            }

            if (source.has("chat"))
                this.chat = ChatDisplay(gson.fromJson(source["chat"].asJsonObject, DisplayConfig::class.java))
            else
                this.chat = ChatDisplay(DisplayConfig().also { it.type = "GUILD_NAME" })

            if (source.has("tab"))
                this.tab = TabDisplay(gson.fromJson(source["tab"].asJsonObject, DisplayConfig::class.java))
            else
                this.tab = TabDisplay(DisplayConfig().also { it.type = "QUESTS" })

            adjustIndices()

            if (shouldSaveCopyNow) saveConfig()

        } catch (e: IOException) {
            Levelhead.logger.error("Failed to initialize display manager.", e)
        }
    }

    fun saveConfig() {
        val jsonObject = JsonObject()

        jsonObject.add("master", gson.toJsonTree(config))
        jsonObject.add("tab", gson.toJsonTree(tab.config))
        jsonObject.add("chat", gson.toJsonTree(chat.config))

        val head = JsonArray()

        this.aboveHead.forEach { display ->
            head.add(gson.toJsonTree(display.config))
        }

        jsonObject.add("head", head)

        try {
            FileUtils.writeStringToFile(file, jsonObject.toString(), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            Levelhead.logger.error("Failed to write to config.", e)
        }
    }

    fun adjustIndices() {
        for (i in aboveHead.indices) {
            aboveHead[i].bottomValue = i == 0
            aboveHead[i].index = i
        }
    }

    fun joinWorld() {
        val displays = mutableListOf(chat, tab).also { it.addAll(aboveHead.filterIndexed{ i, _ -> i <= Levelhead.LevelheadPurchaseStates.aboveHead}) }
        UMinecraft.getWorld()!!.playerEntities
            .map { playerInfo ->
                displays.map {
                Levelhead.LevelheadRequest(playerInfo.uniqueID.trimmed, it,
                    if (it is AboveHeadDisplay) it.bottomValue else false
                )
            } }.flatten().chunked(20).forEach { reqList ->
                Levelhead.fetch(reqList)
            }
    }

    fun playerJoin(player: EntityPlayer) {
        if (player.isNPC) return
        val displays = mutableListOf(chat, tab).also { it.addAll(aboveHead.filterIndexed{ i, _ -> i <= Levelhead.LevelheadPurchaseStates.aboveHead}) }
        displays.filter { !it.cache.containsKey(player.uniqueID) }
            .map { Levelhead.LevelheadRequest(player.uniqueID.trimmed, it,
                if (it is AboveHeadDisplay) it.bottomValue else false
            ) }.ifEmpty { return }.run { Levelhead.fetch(this) }
    }

    fun update() {
        aboveHead.forEachIndexed { i, head ->
            if (i == 0 && Levelhead.LevelheadPurchaseStates.customLevelhead) return@forEachIndexed
            if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
            head.cache.forEach { (_, tag) ->
                if (tag.owner == UPlayer.getUUID() && i == 0 &&
                            Levelhead.LevelheadPurchaseStates.customLevelhead
                        ) return@forEach
                tag.header.run {
                    this.chroma = head.config.headerChroma
                    this.color = head.config.headerColor
                    this.value = "${head.config.headerString}: "
                }
                tag.footer.run {
                    this.chroma = head.config.footerChroma
                    this.color = head.config.footerColor
                }
            }
        }
        chat.run {
            this.cache.forEach { (_, tag) ->
                tag.header.let {
                    it.chroma = this.config.headerChroma
                    it.color = this.config.headerColor
                }
                tag.footer.let {
                    it.chroma = this.config.footerChroma
                    it.color = this.config.footerColor
                }
            }
        }
        tab.run {
            this.cache.forEach { (_, tag) ->
                tag.header.let {
                    it.chroma = this.config.headerChroma
                    it.color = this.config.headerColor
                }
                tag.footer.let {
                    it.chroma = this.config.footerChroma
                    it.color = this.config.footerColor
                }
            }
        }
    }

    fun checkCacheSizes() {
        aboveHead.forEachIndexed { i, head ->
            if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
            head.checkCacheSize()
        }
        chat.checkCacheSize()
        tab.checkCacheSize()
    }

    fun clearCache() {
        aboveHead.forEachIndexed { i, head ->
            if (i > Levelhead.LevelheadPurchaseStates.aboveHead) return@forEachIndexed
            head.cache.clear()
        }
        chat.cache.clear()
        tab.cache.clear()
        joinWorld()
    }
}