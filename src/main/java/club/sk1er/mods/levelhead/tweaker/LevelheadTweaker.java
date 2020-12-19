package club.sk1er.mods.levelhead.tweaker;

import club.sk1er.mods.levelhead.forge.FMLLoadingPlugin;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;
import net.modcore.loader.ModCoreSetupTweaker;

import java.io.File;
import java.util.List;

public class LevelheadTweaker extends ModCoreSetupTweaker {

    public LevelheadTweaker() {
        super(new String[]{FMLLoadingPlugin.class.getName()});
    }
}
