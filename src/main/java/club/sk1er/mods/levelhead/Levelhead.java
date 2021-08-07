package club.sk1er.mods.levelhead;

import club.sk1er.mods.levelhead.auth.MojangAuth;
import club.sk1er.mods.levelhead.commands.LevelheadCommand;
import club.sk1er.mods.levelhead.display.AboveHeadDisplay;
import club.sk1er.mods.levelhead.display.DisplayConfig;
import club.sk1er.mods.levelhead.display.DisplayManager;
import club.sk1er.mods.levelhead.display.LevelheadDisplay;
import club.sk1er.mods.levelhead.purchases.LevelheadPurchaseStates;
import club.sk1er.mods.levelhead.renderer.LevelheadAboveHeadRender;
import club.sk1er.mods.levelhead.renderer.LevelheadChatRenderer;
import club.sk1er.mods.levelhead.renderer.LevelheadTag;
import club.sk1er.mods.levelhead.renderer.NullLevelheadTag;
import gg.essential.api.EssentialAPI;
import gg.essential.api.utils.JsonHolder;
import gg.essential.api.utils.Multithreading;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.Color;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod(modid = Levelhead.MODID, name = "Levelhead", version = Levelhead.VERSION)
public class Levelhead {

    public static final String MODID = "level_head";
    public static final String VERSION = "7.3.0";
    public static final String CHAT_PREFIX = EnumChatFormatting.RED + "[Levelhead] ";

    @Mod.Instance(MODID)
    public static Levelhead INSTANCE;

    private long waitUntil = System.currentTimeMillis();
    private int updates = 0;

    private MojangAuth auth;
    private JsonHolder types = new JsonHolder();
    private JsonHolder paidData = new JsonHolder();
    private JsonHolder purchaseStatus = new JsonHolder();
    private JsonHolder rawPurchases = new JsonHolder();
    private DisplayManager displayManager;
    public UUID userUuid = null;

    private final LevelheadPurchaseStates levelheadPurchaseStates = new LevelheadPurchaseStates();
    private final DecimalFormat format = new DecimalFormat("#,###");
    private final Logger logger = LogManager.getLogger();
    private final Minecraft mc = Minecraft.getMinecraft();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JsonHolder config = new JsonHolder();

        try {
            config = new JsonHolder(FileUtils.readFileToString(event.getSuggestedConfigurationFile(), StandardCharsets.UTF_8));
        } catch (Exception e) { //Generalized to fix potential issues
            this.logger.error("Failed to create config.", e);
        }

        displayManager = new DisplayManager(config, event.getSuggestedConfigurationFile());

        Multithreading.runAsync(() -> types = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_config")));
        auth = new MojangAuth();
        Multithreading.runAsync(() -> {
            auth.auth();
        });

        Multithreading.runAsync(this::refreshPurchaseStates);
        Multithreading.runAsync(this::refreshRawPurchases);
        Multithreading.runAsync(this::refreshPaidData);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (auth.isFailed()) {
            EssentialAPI.getMinecraftUtil().sendMessage("An error occurred while logging into Levelhead: " + auth.getFailMessage());
        }
        userUuid = mc.getSession().getProfile().getId();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new LevelheadChatRenderer(this));
        MinecraftForge.EVENT_BUS.register(new LevelheadAboveHeadRender(this));
        EssentialAPI.getCommandRegistry().registerCommand(new LevelheadCommand());
    }

    public synchronized void refreshRawPurchases() {
        rawPurchases = new JsonHolder(rawWithAgent("https://api.sk1er.club/purchases/" + mc.getSession().getProfile().getId().toString()));
    }

    public synchronized void refreshPaidData() {
        paidData = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_data"));
    }

    public synchronized void refreshPurchaseStates() {
        purchaseStatus = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_purchase_status/" + mc.getSession().getProfile().getId().toString()));
        levelheadPurchaseStates.setChat(purchaseStatus.optBoolean("chat"));
        levelheadPurchaseStates.setTab(purchaseStatus.optBoolean("tab"));
        levelheadPurchaseStates.setExtraHead(purchaseStatus.optInt("head"));
        DisplayManager displayManager = this.displayManager;

        while (displayManager.getAboveHead().size() <= levelheadPurchaseStates.getExtraHead()) {
            displayManager.getAboveHead().add(new AboveHeadDisplay(new DisplayConfig()));
        }

        displayManager.adjustIndexes();
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START
            || !EssentialAPI.getMinecraftUtil().isHypixel()
            || displayManager == null
            || displayManager.getMasterConfig() == null
            || !displayManager.getMasterConfig().isEnabled()) {
            return;
        }

        if (!mc.isGamePaused() && mc.thePlayer != null && mc.theWorld != null) {
            if (System.currentTimeMillis() < waitUntil) {
                if (updates > 0) updates = 0;
                return;
            }

            displayManager.tick();
        }
    }

    public String rawWithAgent(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V" + VERSION + ")");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            try (InputStream is = connection.getInputStream()) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            this.logger.error("Failed to fetch url: {}", url, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return new JsonHolder().put("success", false).put("cause", "API_DOWN").toString();
    }

    private String trimUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public void fetch(final UUID uuid, LevelheadDisplay display, boolean allowOverride) {
        if (updates >= 100) {
            waitUntil = System.currentTimeMillis() + 1000L;
            updates = 0;
            return;
        }

        updates++;
        display.getCache().put(uuid, new NullLevelheadTag(null));
        String type = display.getConfig().getType();

        if (purchaseStatus.has(type) && !purchaseStatus.optBoolean(type)) {
            JsonHolder fakeValue = new JsonHolder()
                .put("header", "Error")
                .put("strlevel", "Item '" + type + "' not purchased. If you believe this is an error, contact Sk1er")
                .put("success", true);
            display.getCache().put(uuid, buildTag(fakeValue, uuid, display, allowOverride));
            return;
        }

        Multithreading.runAsync(() -> {
            final String url = "https://api.sk1er.club/levelheadv5/" + trimUuid(uuid) + "/" + type
                + "/" + trimUuid(mc.getSession().getProfile().getId()) +
                "/" + VERSION + "/" + auth.getHash() + "/" + display.getPosition().name();
            JsonHolder object = new JsonHolder(rawWithAgent(url));

            if (!object.optBoolean("success")) {
                object.put("strlevel", "Error");
            }

            if (!allowOverride) {
                object.put("strlevel", object.optString("level"));
                object.remove("header_obj");
                object.remove("footer_obj");
            }

            LevelheadTag value = buildTag(object, uuid, display, allowOverride);
            display.getCache().put(uuid, value);
            display.getTrueValueCache().put(uuid, object.optString("strlevel"));
        });

        Multithreading.getPool().submit(this::clearCache);
    }

    public LevelheadTag buildTag(JsonHolder object, UUID uuid, LevelheadDisplay display, boolean allowOverride) {
        LevelheadTag value = new LevelheadTag(uuid);
        JsonHolder headerObj = new JsonHolder();
        JsonHolder footerObj = new JsonHolder();
        JsonHolder construct = new JsonHolder();

        //Support for serverside override for Custom Levelhead
        //Apply values from server if present
        if (object.has("header_obj") && allowOverride) {
            headerObj = object.optJSONObject("header_obj");
            headerObj.put("custom", true);
        }

        if (object.has("footer_obj") && allowOverride) {
            footerObj = object.optJSONObject("footer_obj");
            footerObj.put("custom", true);
        }

        if (object.has("header") && allowOverride) {
            headerObj.put("header", object.optString("header"));
            headerObj.put("custom", true);
        }

        //Get config based values and merge
        headerObj.merge(display.getHeaderConfig(), !allowOverride);
        footerObj.merge(display.getFooterConfig().put("footer", object.defaultOptString("strlevel", format.format(object.optInt("level")))), !allowOverride);

        //Ensure text values are present
        construct.put("exclude", object.optBoolean("exclude"));
        construct.put("header", headerObj).put("footer", footerObj);
        construct.put("exclude", object.optBoolean("exclude"));
        construct.put("custom", object.optJSONObject("custom"));
        value.construct(construct);
        return value;
    }

    public Map<String, String> allowedTypes() {
        HashMap<String, String> data = new HashMap<>();
        List<String> keys = types.getKeys();

        for (String key : keys) {
            data.put(key, types.optJSONObject(key).optString("name"));
        }

        JsonHolder stats = paidData.optJSONObject("stats");

        for (String s : stats.getKeys()) {
            if (purchaseStatus.optBoolean(s)) {
                data.put(s, stats.optJSONObject(s).optString("name"));
            }
        }

        return data;
    }

    //Remote runaway memory leak from storing levels in ram.
    private void clearCache() {
        displayManager.checkCacheSizes();
    }

    public static int getRGBColor() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.8f);
    }

    public static int getRGBDarkColor() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.2f);
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    public LevelheadPurchaseStates getLevelheadPurchaseStates() {
        return levelheadPurchaseStates;
    }

    public JsonHolder getPurchaseStatus() {
        return purchaseStatus;
    }

    public MojangAuth getAuth() {
        return auth;
    }

    public JsonHolder getPaidData() {
        return paidData;
    }

    public JsonHolder getRawPurchases() {
        return rawPurchases;
    }

    public JsonHolder getTypes() {
        return types;
    }

    public Logger getLogger() {
        return logger;
    }
}
