package club.sk1er.mods.levelhead.display;

import java.util.HashSet;
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
        cache.forEach((uuid, levelheadTag) -> {
            if (System.currentTimeMillis() - levelheadTag.getTime() > TimeUnit.SECONDS.toMillis(15)) {
                if (!levelheadTag.getFooter().getValue().equalsIgnoreCase("NONE"))
                    remove.add(uuid);
            }
        });
        for (UUID uuid : remove) {
            cache.remove(uuid);
        }
        super.tick();
    }
}
