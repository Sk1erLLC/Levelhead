package club.sk1er.mods.levelhead;

import club.sk1er.mods.core.util.JsonHolder;
import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.core.util.Multithreading;
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
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Levelhead extends DummyModContainer {

    public static final String MODID = "level_head";
    public static final String VERSION = "7.0.1";
    public static final String CHAT_PREFIX = EnumChatFormatting.RED + "[Levelhead] ";
    private static Levelhead instance;
    public UUID userUuid = null;
    public int count = 100;
    public int wait = 1;
    private long waitUntil = System.currentTimeMillis();
    private int updates = 0;
    private MojangAuth auth;
    private JsonHolder types = new JsonHolder();
    private DecimalFormat format = new DecimalFormat("#,###");
    private JsonHolder paidData = new JsonHolder();
    private DisplayManager displayManager;
    private LevelheadPurchaseStates levelheadPurchaseStates = new LevelheadPurchaseStates();
    private JsonHolder purchaseStatus = new JsonHolder();
    private LevelheadChatRenderer levelheadChatRenderer;
    private JsonHolder rawPurchases = new JsonHolder();

    public Levelhead() {
        super(new ModMetadata());

        ModMetadata meta = this.getMetadata();
        meta.modId = MODID;
        meta.version = VERSION;

        meta.name = "Sk1er Level Head";
        meta.description = "Levelhead displays a player's network level above their head";

        //noinspection deprecation
        meta.url = meta.updateUrl = "http://sk1er.club/levelhead";

        meta.authorList = Collections.singletonList("Sk1erLLC");
        meta.credits = "HypixelAPI";
    }

    public static int getRGBColor() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.8f);
    }

    public static int getRGBDarkColor() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.2f);
    }

    public static Levelhead getInstance() {
        return instance;
    }

    public DisplayManager getDisplayManager() {
        return displayManager;
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller) {
        bus.register(this);
        return true;
    }

    public LevelheadPurchaseStates getLevelheadPurchaseStates() {
        return levelheadPurchaseStates;
    }

    public JsonHolder getPurchaseStatus() {
        return purchaseStatus;
    }

    public synchronized void refreshRawPurchases() {
        rawPurchases = new JsonHolder(rawWithAgent("https://api.sk1er.club/purchases/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString()));
    }

    public MojangAuth getAuth() {
        return auth;
    }

    public synchronized void refreshPaidData() {
        paidData = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_data"));

    }

    public JsonHolder getPaidData() {
        return paidData;
    }

    public JsonHolder getRawPurchases() {
        return rawPurchases;
    }

    public synchronized void refreshPurchaseStates() {
        purchaseStatus = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_purchase_status/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString()));
        levelheadPurchaseStates.setChat(purchaseStatus.optBoolean("chat"));
        levelheadPurchaseStates.setTab(purchaseStatus.optBoolean("tab"));
        levelheadPurchaseStates.setExtraHead(purchaseStatus.optInt("head"));
        DisplayManager displayManager = this.displayManager;
        while (displayManager.getAboveHead().size() <= levelheadPurchaseStates.getExtraHead()) {
            displayManager.getAboveHead().add(new AboveHeadDisplay(new DisplayConfig()));
        }
        displayManager.adjustIndexes();

    }
    @Subscribe
    @EventHandler
    public void init(FMLInitializationEvent event) {
        ModCoreInstaller.initializeModCore(Minecraft.getMinecraft().mcDataDir);
    }

    @Subscribe
    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        JsonHolder config = new JsonHolder();

        try {
            config = new JsonHolder(FileUtils.readFileToString(event.getSuggestedConfigurationFile()));
        } catch (Exception e) { //Generalized to fix potential issues
            e.printStackTrace();
        }

        displayManager = new DisplayManager(config, event.getSuggestedConfigurationFile());

        Multithreading.runAsync(() -> types = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_config")));
        auth = new MojangAuth();
        Multithreading.runAsync(() -> {
            auth.auth();
            if (auth.isFailed()) {
                MinecraftUtils.sendMessage("An error occurred while logging logging into Levelhead: " + auth.getFailMessage());
            }
        });

        Multithreading.runAsync(this::refreshPurchaseStates);
        Multithreading.runAsync(this::refreshRawPurchases);
        Multithreading.runAsync(this::refreshPaidData);

    }

    @Subscribe
    @EventHandler
    public void init(FMLPostInitializationEvent event) {
        instance = this;
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        userUuid = minecraft.getSession().getProfile().getId();
        register(new LevelheadAboveHeadRender(this), this);
        ClientCommandHandler.instance.registerCommand(new LevelheadCommand());
        levelheadChatRenderer = new LevelheadChatRenderer(this);
        register(levelheadChatRenderer);
    }


    public JsonHolder getTypes() {
        return types;
    }


    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void tick(TickEvent.ClientTickEvent event) {

        if (event.phase == TickEvent.Phase.START
                || !MinecraftUtils.isHypixel()
                || displayManager == null
                || displayManager.getMasterConfig() == null
                || !displayManager.getMasterConfig().isEnabled()) {

            return;
        }


        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isGamePaused() && mc.thePlayer != null && mc.theWorld != null) {
            if (System.currentTimeMillis() < waitUntil) {
                if (updates > 0) {
                    updates = 0;
                }
                return;
            }
            displayManager.tick();

        }
    }

    public String rawWithAgent(String url) {
        System.out.println("Fetching: " + url);
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (SK1ER LEVEL HEAD V" + VERSION + ")");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            InputStream is = connection.getInputStream();
            return IOUtils.toString(is, Charset.defaultCharset());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JsonHolder().put("success", false).put("cause", "API_DOWN").toString();
    }


    private String trimUuid(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public void fetch(final UUID uuid, LevelheadDisplay display, boolean allowOverride) {
        if (updates >= count) {
            waitUntil = System.currentTimeMillis() + 1000 * wait;
            updates = 0;
            return;
        }
        updates++;
        display.getCache().put(uuid, new NullLevelheadTag(null));
        String type = display.getConfig().getType();

        if (purchaseStatus.has(type) && !purchaseStatus.optBoolean(type)) {
            JsonHolder fakeValue = new JsonHolder();
            fakeValue.put("header", "Error");
            fakeValue.put("strlevel", "Item '" + type + "' not purchased. If you believe this is an error, contact Sk1er");
            fakeValue.put("success", true);
            display.getCache().put(uuid, buildTag(fakeValue, uuid, display, allowOverride));
            return;
        }
        Multithreading.runAsync(() -> {
            String raw = rawWithAgent(
                    "https://api.sk1er.club/levelheadv5/" + trimUuid(uuid) + "/" + type
                            + "/" + trimUuid(Minecraft.getMinecraft().getSession().getProfile().getId()) +
                            "/" + VERSION + "/" + auth.getHash() + "/" + display.getPosition().name());
            JsonHolder object = new JsonHolder(raw);
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
        Multithreading.POOL.submit(this::clearCache);
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

    public HashMap<String, String> allowedTypes() {
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

    public LevelheadTag getLevelString(LevelheadDisplay display, UUID uuid) {
        return display.getCache().getOrDefault(uuid, null);
    }

    //Remote runaway memory leak from storing levels in ram.
    private void clearCache() {
        displayManager.checkCacheSizes();
    }

    private void register(Object... events) {
        for (Object o : events) {
            MinecraftForge.EVENT_BUS.register(o);
        }
    }


}
