package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.Levelhead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatDisplay extends LevelheadDisplay {

    public ChatDisplay(DisplayConfig config) {
        super(DisplayPosition.CHAT, config);
    }

    @Override
    public void tick() {
        if (Levelhead.INSTANCE.getLevelheadPurchaseStates().isChat()) {
            for (NetworkPlayerInfo networkPlayerInfo : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                UUID id = networkPlayerInfo.getGameProfile().getId();
                if (id != null && !cache.containsKey(id)) {
                    Levelhead.INSTANCE.fetch(id, this, false);
                }
            }
        }
    }

    @Override
    public void checkCacheSize() {
        if (cache.size() > Math.max(Levelhead.INSTANCE.getDisplayManager().getMasterConfig().getPurgeSize(), 150)) {
            Set<UUID> safePlayers = new HashSet<>();
            for (NetworkPlayerInfo info : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                UUID id = info.getGameProfile().getId();
                if (existedMoreThan5Seconds.contains(id)) {
                    safePlayers.add(id);
                }
            }

            existedMoreThan5Seconds.clear();
            existedMoreThan5Seconds.addAll(safePlayers);

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
        existedMoreThan5Seconds.clear();
    }
}
