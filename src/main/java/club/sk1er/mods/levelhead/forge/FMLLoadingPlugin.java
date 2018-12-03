package club.sk1er.mods.levelhead.forge;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.forge.transform.ClassTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion()
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
