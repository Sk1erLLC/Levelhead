package club.sk1er.mods.levelhead.renderer;

import java.util.UUID;

public class NullLevelheadTag extends LevelheadTag {


    public NullLevelheadTag(UUID owner) {
        super(owner);
    }


    @Override
    public String toString() {
        return null;
    }
}
