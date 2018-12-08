package club.sk1er.mods.levelhead.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Mitchell Katz on 6/8/2017.
 */
public class Sk1erMod {
    /*
        Sk1erMod 5.0
        Dabbing intensifies
     */
    private static Sk1erMod instance;
    private boolean first = false;
    private List<IChatComponent> updateMessage = new ArrayList<>();
    private String modid;
    private String version;
    private boolean enabled = true;
    private boolean hasUpdate = false;
    private String name;
    private String apiKey;
    private String prefix;
    private JsonHolder en;
    private boolean hypixel;
    private GenKeyCallback callback;
    private ConcurrentLinkedQueue<IChatComponent> messages = new ConcurrentLinkedQueue<>();
    private boolean bookUser = false;
    private boolean firstFileStatus = false;
    private File dir;
    private boolean book = false;

    public Sk1erMod(String modid, String version, String name) {
        this.modid = modid;
        this.version = version;
        this.name = name;
        instance = this;
        prefix = EnumChatFormatting.RED + "[" + EnumChatFormatting.AQUA + this.name + EnumChatFormatting.RED + "]" + EnumChatFormatting.YELLOW + ": ";
        MinecraftForge.EVENT_BUS.register(this);
        File mcDataDir = Minecraft.getMinecraft().mcDataDir;

        dir = new File(mcDataDir, "sk1ermod");
        if (!dir.exists())
            dir.mkdirs();
    }

    public Sk1erMod(String modid, String version, String name, GenKeyCallback callback) {
        this(modid, version, name);
        this.callback = callback;
    }

    public static Sk1erMod getInstance() {
        return instance;
    }

    public boolean isHypixel() {
        return hypixel;
    }

    public JsonHolder getResponse() {
        return en;
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public boolean isEnabled() {
        return true;
    }

    public List<IChatComponent> getUpdateMessage() {
        return updateMessage;
    }

    public String getApIKey() {
        return apiKey;
    }

    public void sendMessage(String message) {
        this.messages.add(ForgeHooks.newChatWithLinks(prefix + message));
    }

    @SubscribeEvent
    public void tick(TickEvent.RenderTickEvent event) {


        if (Minecraft.getMinecraft().thePlayer == null) return;
        while (!messages.isEmpty()) {
            Minecraft.getMinecraft().thePlayer.addChatComponentMessage(messages.poll());
        }
        if (book) {
            book = false;
        }
    }

    public JsonObject getPlayer(String name) {
        return new JsonParser().parse(rawWithAgent("http://sk1er.club/data/" + name + "/" + getApIKey())).getAsJsonObject();
    }

    public void checkStatus() {
        Multithreading.schedule(() -> {
            en = new JsonHolder(rawWithAgent("http://sk1er.club/genkey?name=" + Minecraft.getMinecraft().getSession().getProfile().getName()
                    + "&uuid=" + Minecraft.getMinecraft().getSession().getPlayerID().replace("-", "")
                    + "&mcver=" + Minecraft.getMinecraft().getVersion()
                    + "&modver=" + version
                    + "&mod=" + modid
            ));
            if (callback != null)
                callback.call(en);
            System.out.println(en);
            updateMessage.clear();
            enabled = en.optBoolean("enabled");
            hasUpdate = en.optBoolean("update");
            apiKey = en.optString("key");

            first = en.optBoolean("first");
            checkFirst(en.optString("lock"), first);
            if (hasUpdate) {
                process(prefix + "----------------------------------");

                process(" ");
                process(prefix + "            " + name + " is out of date!");
                process(prefix + "Update level: " + en.optString("level"));
                process(prefix + "Update URL: " + en.optString("url"));
                process(prefix + "Message from Sk1er: ");
                process(prefix + en.optString("message"));
                process(" ");

                process(prefix + "----------------------------------");
            }


        }, 0, 5, TimeUnit.MINUTES);
    }

    private void process(String input) {

        updateMessage.add(ForgeHooks.newChatWithLinks(input));
    }

    private void checkFirst(String lock, boolean first) {
        if (lock.isEmpty())
            return;
        File tmp = new File(dir, lock);
        if (!tmp.exists()) {
            try {
                tmp.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.firstFileStatus = first;
        }

    }

    @SubscribeEvent
    public void onLoin(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        hypixel = !FMLClientHandler.instance().getClient().isSingleplayer()
                && (FMLClientHandler.instance().getClient().getCurrentServerData().serverIP.contains("hypixel.net") ||
                FMLClientHandler.instance().getClient().getCurrentServerData().serverName.equalsIgnoreCase("HYPIXEL"));
        if (hasUpdate() || first) {
            Multithreading.runAsync(() -> {
                while (Minecraft.getMinecraft().thePlayer == null) {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (first && firstFileStatus) {
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.book = true;

                }
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for (IChatComponent s : getUpdateMessage()) {
                    Minecraft.getMinecraft().thePlayer.addChatComponentMessage(s);
                }
            });
        }


    }


    @SubscribeEvent
    public void onPlayerLogOutEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        hypixel = false;
    }

    public String rawWithAgent(String url) {
        url = url.replace(" ", "%20");
        System.out.println("Fetching " + url);
        try {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(true);
            connection.addRequestProperty("User-Agent", "Mozilla/4.76 (" + modid + " V" + version + ")");
            connection.setReadTimeout(15000);
            connection.setConnectTimeout(15000);
            connection.setDoOutput(true);
            InputStream is = connection.getInputStream();
            return IOUtils.toString(is, Charset.defaultCharset());

        } catch (Exception e) {
            e.printStackTrace();
        }
        JsonObject object = new JsonObject();
        object.addProperty("success", false);
        object.addProperty("cause", "Exception");
        return object.toString();

    }


}
