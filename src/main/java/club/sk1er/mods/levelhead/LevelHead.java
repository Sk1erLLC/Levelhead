package club.sk1er.mods.levelhead;

import club.sk1er.mods.levelhead.commands.ToggleCommand;
import club.sk1er.mods.levelhead.renderer.LevelHeadRender;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.Multithreading;
import club.sk1er.mods.levelhead.utils.Sk1erMod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Team;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(modid = LevelHead.MODID, version = LevelHead.VERSION, clientSideOnly = true)
public class LevelHead {

    public static final String HEY_DEVS = "This code was adapted from codename_B's facepixel mod";
    public static final String MODID = "LEVEL_HEAD";
    public static final String VERSION = "3.1.1";
    public static boolean TOGGLED_ON = true;
    public static String PRIMARY_COLOR = "";
    public static String SECOND_COLOR = "";
    public static String PREFIX = "Level";
    public static UUID UUID = null;
    public static Map<UUID, String> stringCache = new HashMap<>();
    public static int count = 1;
    public static int wait = 60;

    public static boolean PRIMARY_CHROMA;
    public static boolean SECONDARY_CHROMA;

    //    private static Map<String, Integer> levelCache = new HashMap<>();
    private static LevelHead instance;
    private long waitUntil = System.currentTimeMillis();
    private int updates = 0;
    private JsonObject config = new JsonObject();
    private Sk1erMod mod;
    private File configFile;
    private java.util.List<UUID> confirmedNotAurabot = new ArrayList<>();
    private HashMap<UUID, Integer> timeCheck = new HashMap<>();

    /*
     * This code is not that great but works so yay!
     */

    public static int getColor() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.8f);
    }

    public static int getColorDark() {
        return Color.HSBtoRGB(System.currentTimeMillis() % 1000L / 1000.0f, 0.8f, 0.2f);
    }

    public static LevelHead getInstance() {
        return instance;
    }

    @EventHandler
    public void init(FMLPreInitializationEvent event) {
        mod = new Sk1erMod(MODID, VERSION, "Level Head");
        mod.checkStatus();
        this.configFile = event.getSuggestedConfigurationFile();
        register(mod);
    }


    @EventHandler
    public void init(FMLPostInitializationEvent event) {
        instance = this;
        Minecraft minecraft = FMLClientHandler.instance().getClient();
        UUID = minecraft.getSession().getProfile().getId();

        register(new LevelHeadRender(this), this);

        File f = configFile;
        if (f.exists()) {
            try {
                try {
                    FileReader fr = new FileReader(f);
                    BufferedReader br = new BufferedReader(fr);
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null)
                        builder.append(line);

                    String done = builder.toString();
                    boolean failed = false;
                    try {
                        this.config = new JsonParser().parse(done).getAsJsonObject();
                    } catch (Exception e) {
                        config = new JsonObject();
                        failed = true;
                    }
                    if (failed)
                        saveConfig();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {

            }
        }
        TOGGLED_ON = !config.has("toggled") || config.get("toggled").getAsBoolean();
        PRIMARY_CHROMA = config.has("primary_chroma") && config.get("primary_chroma").getAsBoolean();
        SECONDARY_CHROMA = config.has("secondary_chroma") && config.get("secondary_chroma").getAsBoolean();
        PRIMARY_COLOR = config.has("primary") && !config.get("primary").getAsString().isEmpty() ? config.get("primary").getAsString() : ChatColor.AQUA.toString();
        SECOND_COLOR = config.has("secondary") && !config.get("secondary").getAsString().isEmpty() ? config.get("secondary").getAsString() : ChatColor.YELLOW.toString();
        PREFIX = config.has("prefix") && !config.get("prefix").getAsString().isEmpty() ? config.get("prefix").getAsString() : "Level";

        ClientCommandHandler.instance.registerCommand(new ToggleCommand());
    }

    public void saveConfig() {

        File f = configFile;
        try {
            f.createNewFile();
            FileWriter fw = new FileWriter(f);
            BufferedWriter bw = new BufferedWriter(fw);
            config = new JsonObject();
            config.addProperty("toggled", TOGGLED_ON);
            config.addProperty("primary_chroma", PRIMARY_CHROMA);
            config.addProperty("secondary_chroma", SECONDARY_CHROMA);
            config.addProperty("primary", PRIMARY_COLOR);
            config.addProperty("secondary", SECOND_COLOR);
            config.addProperty("prefix", PREFIX);

            bw.write(config.toString());
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean loadOrRender(EntityPlayer player) {
        if (!(mod.isHypixel() && TOGGLED_ON))
            return false;
        for (PotionEffect effect : player.getActivePotionEffects()) { // TODO - Method obfuscated (PORTING REQUIRED)
            if (effect.getPotionID() == 14)
                return false;
        }
        return confirmedNotAurabot.contains(player.getUniqueID()) &&
                player.riddenByEntity == null &&
                renderFromTeam(player) &&
                !(player.hasCustomName() &&
                        player.getCustomNameTag().isEmpty()) &&
                !player.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer) &&
                !player.isSneaking() &&
                player.getAlwaysRenderNameTagForRender() &&
                !player.getDisplayNameString().isEmpty() &&
                !player.isInvisible() &&
                player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) < 64 * 64;


    }

    private boolean renderFromTeam(EntityPlayer player) {
        Team team = player.getTeam();
        Team team1 = Minecraft.getMinecraft().thePlayer.getTeam();

        if (team != null) {
            Team.EnumVisible enumVisible = team.getNameTagVisibility();
            switch (enumVisible) {
                case ALWAYS:
                    return true;
                case NEVER:
                    return false;
                case HIDE_FOR_OTHER_TEAMS:
                    return team1 == null || team.isSameTeam(team1);
                case HIDE_FOR_OWN_TEAM:
                    return team1 == null || !team.isSameTeam(team1);
                default:
                    return true;
            }
        }
        return true;
    }

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START || !mod.isHypixel() || !TOGGLED_ON || !mod.isEnabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isGamePaused() && mc.thePlayer != null && mc.theWorld != null) {
            if (System.currentTimeMillis() < waitUntil) {
                if (updates > 0) {
                    updates = 0;
                }
                return;
            }
            for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                if (!confirmedNotAurabot.contains(entityPlayer.getUniqueID())) {
                    if (!timeCheck.containsKey(entityPlayer.getUniqueID()))
                        timeCheck.put(entityPlayer.getUniqueID(), 0);
                    int old = timeCheck.get(entityPlayer.getUniqueID());
                    if (old > 100) {
                        if (!confirmedNotAurabot.contains(entityPlayer.getUniqueID()))
                            confirmedNotAurabot.add(entityPlayer.getUniqueID());
                    } else if (!entityPlayer.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer))
                        timeCheck.put(entityPlayer.getUniqueID(), old + 1);
                }

                if (loadOrRender(entityPlayer)) {
                    final UUID uuid = entityPlayer.getUniqueID();
                    if (!stringCache.containsKey(uuid)) {
                        getLevel(uuid);
                    }
                }
            }
        }
    }

    public String rawWithAgent(String url) {
        System.out.println("Fetching " + url);
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
        return new JSONObject().put("success", false).put("cause", "API_DOWN").toString();
    }

    private void getLevel(final UUID uuid) {
        if (updates >= count) {
            waitUntil = System.currentTimeMillis() + 1000 * wait;
            return;
        }
        updates++;
        stringCache.put(uuid, "");
        Multithreading.runAsync(() -> {
            JSONObject object = new JSONObject(rawWithAgent("http://sk1er.club/newlevel/" + uuid.toString().replace("-", "") + "/" + VERSION));
            stringCache.put(uuid, object.optString("header", LevelHead.PREFIX) + ": " + object.optString("strlevel", object.getInt("level") + ""));
        });
        Multithreading.POOL.submit(this::clearCache);
    }

    public String getLevelString(UUID uuid) {
        if (stringCache.containsKey(uuid)) {
            return stringCache.get(uuid);
        } else {
            return LevelHead.PREFIX + ": -1";
        }
    }

    private void clearCache() {
        if (stringCache.size() > 1000) {
            ArrayList<UUID> safePlayers = new ArrayList<>();
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (confirmedNotAurabot.contains(player.getUniqueID())) {
                    safePlayers.add(player.getUniqueID());
                }
            }
            confirmedNotAurabot.clear();
            confirmedNotAurabot.addAll(safePlayers);

            for (UUID uuid : stringCache.keySet()) {
                if (!safePlayers.contains(uuid)) {
                    stringCache.remove(uuid);
                }
            }
            System.out.println("Cache cleared!");
        }
    }

    private void register(Object... events) {
        for (Object o : events) {
            MinecraftForge.EVENT_BUS.register(o);
        }
    }
}
