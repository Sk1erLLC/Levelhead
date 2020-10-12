package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.renderer.LevelheadTag;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SongDisplay extends AboveHeadDisplay {
    public SongDisplay(DisplayConfig config) {
        super(config);
        config.setType("SONG");
    }

    @Override
    public void tick() {
        Set<UUID> remove = new HashSet<>();
        for (Map.Entry<UUID, LevelheadTag> entry : cache.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                return;
            }

            if (System.currentTimeMillis() - entry.getValue().getTime() > TimeUnit.SECONDS.toMillis(15)) {
                if (!entry.getValue().getFooter().getValue().equalsIgnoreCase("NONE")) {
                    remove.add(entry.getKey());
                }
            }
        }

        for (UUID uuid : remove) {
            cache.remove(uuid);
        }

        super.tick();
    }
}
