package club.sk1er.mods.levelhead.tweaker;

import club.sk1er.mods.levelhead.ModCoreInstaller;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.ForgeVersion;

import java.io.File;
import java.util.List;

public class LaunchTweaker implements ITweaker {
    private File gameDir = null;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        this.gameDir = gameDir;
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        //Minecraft Version
        String version = "unknown";

        //#if FORGE
        try {
            version = "forge_"+ForgeVersion.class.getDeclaredField("mcVersion").get(null) ;
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }

        //#else
        //$$ version=net.minecraft.MinecraftVersion.create().getName()+"_fabric";
        //#endif
        int initialize = ModCoreInstaller.initialize(gameDir, version);
        System.out.println("ModCore Init Status From Scrollable Tooltips " + initialize);
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
