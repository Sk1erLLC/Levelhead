package club.sk1er.mods.levelhead.tweaker;

import club.sk1er.mods.levelhead.forge.FMLLoadingPlugin;
import gg.essential.loader.EssentialSetupTweaker;

@SuppressWarnings("unused")
public class LevelheadTweaker extends EssentialSetupTweaker {
    public LevelheadTweaker() {
        super(new String[]{FMLLoadingPlugin.class.getName()});
    }
}
