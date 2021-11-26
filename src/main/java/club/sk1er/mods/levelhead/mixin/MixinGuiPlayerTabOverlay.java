package club.sk1er.mods.levelhead.mixin;

import club.sk1er.mods.levelhead.render.TabRender;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {

    @Unique
    private NetworkPlayerInfo levelhead$playerInfo;

    @Inject(method = "drawPing", at = @At("HEAD"))
    private void levelhead$drawPingHook(int offset, int x, int y, NetworkPlayerInfo networkPlayerInfoIn, CallbackInfo ci) {
        TabRender.INSTANCE.drawPingHook(offset, x, y, networkPlayerInfoIn);
    }

    @ModifyVariable(method = "renderPlayerlist", at = @At("STORE"))
    private NetworkPlayerInfo levelhead$capturePlayerInfo(NetworkPlayerInfo playerInfo) {
        this.levelhead$playerInfo = playerInfo;
        return playerInfo;
    }

    @ModifyVariable(method = "renderPlayerlist", ordinal = 3, at = @At("STORE"))
    private int levelhead$tabWidthHook(int in) {
        return in + TabRender.INSTANCE.getLevelheadWidth(levelhead$playerInfo);
    }
}
