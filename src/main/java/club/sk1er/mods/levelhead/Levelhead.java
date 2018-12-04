package club.sk1er.mods.levelhead;

import club.sk1er.mods.levelhead.auth.MojangAuth;
import club.sk1er.mods.levelhead.commands.LevelheadCommand;
import club.sk1er.mods.levelhead.display.DisplayManager;
import club.sk1er.mods.levelhead.display.LevelheadDisplay;
import club.sk1er.mods.levelhead.purchases.LevelheadPurchaseStates;
import club.sk1er.mods.levelhead.renderer.LevelheadAboveHeadRender;
import club.sk1er.mods.levelhead.renderer.LevelheadChatRenderer;
import club.sk1er.mods.levelhead.renderer.LevelheadTag;
import club.sk1er.mods.levelhead.renderer.NullLevelheadTag;
import club.sk1er.mods.levelhead.utils.JsonHolder;
import club.sk1er.mods.levelhead.utils.Multithreading;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.UUID;

public class Levelhead extends DummyModContainer {


    /*
        Hello !
     */
    public static final String MODID = "LEVEL_HEAD";
    public static final String VERSION = "6.0";
    private static Levelhead instance;
    public UUID userUuid = null;
    public int count = 1;
    public int wait = 60;
    private long waitUntil = System.currentTimeMillis();
    private int updates = 0;
    private Sk1erMod mod;

    private MojangAuth auth;
    private JsonHolder types = new JsonHolder();
    private DecimalFormat format = new DecimalFormat("#,###");
    private JsonHolder paidData = new JsonHolder();
    private DisplayManager displayManager;
    private LevelheadPurchaseStates levelheadPurchaseStates = new LevelheadPurchaseStates();
    private JsonHolder purchaseStatus;

    public Levelhead() {
        super(new ModMetadata());

        ModMetadata meta = this.getMetadata();
        meta.modId = MODID;
        meta.version = VERSION;

        meta.name = "Sk1er Level Head";
        meta.description = "Levelhead displays a player's network level above their head";

        //noinspection deprecation
        meta.url = meta.updateUrl = "http://sk1er.club/levelhead";

        meta.authorList = Arrays.asList("Sk1er", "boomboompower");
        meta.credits = "HypixelAPI";
        System.out.println("INITIATED");

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

    public synchronized void refreshPurchaseStates() {
        purchaseStatus = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_purchase_status/" + Minecraft.getMinecraft().getSession().getProfile().getId().toString()));
        levelheadPurchaseStates.setChat(purchaseStatus.optBoolean("chat"));
        levelheadPurchaseStates.setTab(purchaseStatus.optBoolean("tab"));
        levelheadPurchaseStates.setExtraHead(purchaseStatus.optInt("tab"));


    }

    @Subscribe
    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        Multithreading.runAsync(() -> types = new JsonHolder(rawWithAgent("https://api.sk1er.club/levelhead_config")));
        mod = new Sk1erMod(MODID, VERSION, "Levelhead", object -> {
            count = object.optInt("count");
            this.wait = object.optInt("wait", Integer.MAX_VALUE);
            if (count == 0 || wait == Integer.MAX_VALUE) {
                mod.sendMessage("An error occurred whilst loading internal Levelhead info. ");
            }
        });
        mod.checkStatus();
        auth = new MojangAuth(mod);
        Multithreading.runAsync(() -> {
            auth.auth();
            if (auth.isFailed()) {
                System.out.println("FAILED TO AUTH: " + auth.getFailMessage());
            }
        });
        register(mod);
        JsonHolder config = new JsonHolder();
        try {
            config = new JsonHolder(FileUtils.readFileToString(event.getSuggestedConfigurationFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        displayManager = new DisplayManager(config, event.getSuggestedConfigurationFile());
    }

    private LevelheadChatRenderer levelheadChatRenderer;
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

        if ((event.phase == TickEvent.Phase.START ||
                !mod.isHypixel() ||
                !getDisplayManager().getMasterConfig().isEnabled()
                || !mod.isEnabled())) {

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
            getDisplayManager().tick();

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
        Multithreading.runAsync(() -> {
            String raw = rawWithAgent(
                    "https://api.sk1er.club/levelheadv5/" + trimUuid(uuid) + "/" + display.getConfig().getType()
                            + "/" + trimUuid(Minecraft.getMinecraft().getSession().getProfile().getId()) +
                            "/" + VERSION + "/" + auth.getAccessKey() + "/" + display.getPosition().name());
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
            headerObj = object.optJsonObject("header_obj");
            headerObj.put("custom", true);
        }
        if (object.has("footer_obj")&& allowOverride) {
            footerObj = object.optJsonObject("footer_obj");
            footerObj.put("custom", true);
        }
        if (object.has("header")&& allowOverride) {
            headerObj.put("header", object.optString("header"));
            headerObj.put("custom", true);
        }

        //Get config based values and merge
        headerObj.merge(display.getHeaderConfig(), !allowOverride);
        footerObj.merge(display.getFooterConfig().put("footer", object.optString("strlevel", format.format(object.getInt("level")))), !allowOverride);
        //Ensure text values are present
        construct.put("exclude", object.optBoolean("exclude"));
        construct.put("header", headerObj).put("footer", footerObj);
        value.construct(construct);
        return value;
    }


    public LevelheadTag getLevelString(LevelheadDisplay display, UUID uuid) {
        return display.getCache().getOrDefault(uuid, null);
    }

    //Remote runaway memory leak from storing levels in ram.
    private void clearCache() {
        getDisplayManager().checkCacheSizes();
    }

    private void register(Object... events) {
        for (Object o : events) {
            MinecraftForge.EVENT_BUS.register(o);
        }
    }


    public Sk1erMod getSk1erMod() {
        return mod;
    }

}
