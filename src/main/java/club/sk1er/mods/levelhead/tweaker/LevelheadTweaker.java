package club.sk1er.mods.levelhead.tweaker;

import club.sk1er.mods.levelhead.forge.FMLLoadingPlugin;
import gg.essential.loader.EssentialTweaker;

@SuppressWarnings("unused")
public class LevelheadTweaker extends EssentialTweaker {
    public LevelheadTweaker() {
        super(new String[]{FMLLoadingPlugin.class.getName()});
    }
}
