package club.sk1er.mods.levelhead.forge;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.ModCoreInstaller;
import club.sk1er.mods.levelhead.forge.transform.ClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion(ForgeVersion.mcVersion)
@IFMLLoadingPlugin.SortingIndex(1000)
public final class FMLLoadingPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {

        return new String[]{
                ClassTransformer.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return Levelhead.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
