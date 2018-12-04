package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.Levelhead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.ArrayList;
import java.util.UUID;

public class ChatDisplay extends LevelheadDisplay {

    public ChatDisplay(DisplayConfig config) {
        super(DisplayPosition.CHAT, config);
    }

    @Override
    public void tick() {
        for (NetworkPlayerInfo networkPlayerInfo : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
            UUID id = networkPlayerInfo.getGameProfile().getId();
            if (id != null)
                if (!cache.containsKey(id))
                    Levelhead.getInstance().fetch(id, this, false);
        }
    }

    @Override
    public void checkCacheSize() {
        if (cache.size() > Math.max(Levelhead.getInstance().getDisplayManager().getMasterConfig().getPurgeSize(), 150)) {
            ArrayList<UUID> safePlayers = new ArrayList<>();
            for (NetworkPlayerInfo info : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                UUID id = info.getGameProfile().getId();
                if (existedMorethan5Seconds.contains(id)) {
                    safePlayers.add(id);
                }
            }
            existedMorethan5Seconds.clear();
            existedMorethan5Seconds.addAll(safePlayers);


            for (UUID uuid : cache.keySet()) {
                if (!safePlayers.contains(uuid)) {
                    cache.remove(uuid);
                    trueValueCache.remove(uuid);
                }
            }
        }
    }

    @Override
    public void onDelete() {
        cache.clear();
        trueValueCache.clear();
        existedMorethan5Seconds.clear();
    }
}
