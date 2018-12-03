package club.sk1er.mods.levelhead.forge.transform;

import net.minecraft.client.network.NetworkPlayerInfo;

@SuppressWarnings("unused")
public final class Hooks {

    public static void drawPingHook(int pos, int x, int y, NetworkPlayerInfo playerInfo) {
        System.out.println("drawPingHook( " + pos + ", " + x + ", " + y + ", " + playerInfo.toString() + " )");
    }

}
