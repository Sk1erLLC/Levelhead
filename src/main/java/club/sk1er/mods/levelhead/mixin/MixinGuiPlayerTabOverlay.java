package club.sk1er.mods.levelhead.mixin;

import club.sk1er.mods.levelhead.render.TabRender;
import gg.essential.api.EssentialAPI;
import gg.essential.universal.UMinecraft;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {

    @Unique
    private NetworkPlayerInfo levelhead$playerInfo;

    @Inject(method = "drawPing", at = @At("HEAD"))
    private void levelhead$drawPingHook(int offset, int x, int y, NetworkPlayerInfo networkPlayerInfoIn, CallbackInfo ci) {
        if (!EssentialAPI.getMinecraftUtil().isHypixel()) return;
        TabRender.INSTANCE.drawPingHook(offset, x, y, networkPlayerInfoIn);
    }

    @ModifyVariable(method = "renderPlayerlist", index = 9, at = @At("STORE"))
    private NetworkPlayerInfo levelhead$capturePlayerInfo(NetworkPlayerInfo playerInfo) {
        this.levelhead$playerInfo = playerInfo;
        return playerInfo;
    }

    @ModifyArg(method = "renderPlayerlist", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"))
    private String levelhead$tabWidthHook(String in) {
        if (!EssentialAPI.getMinecraftUtil().isHypixel()) return in;
        if (this.levelhead$playerInfo == null) return in;
        return in + StringUtils.repeat(' ', (int) Math.ceil(TabRender.INSTANCE.getLevelheadWidth(this.levelhead$playerInfo) / (double) UMinecraft.getFontRenderer().getCharWidth(' ')));
    }
}
