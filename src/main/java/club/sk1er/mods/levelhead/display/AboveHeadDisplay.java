package club.sk1er.mods.levelhead.display;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.Team;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AboveHeadDisplay extends LevelheadDisplay {

    boolean bottomValue = true;

    public AboveHeadDisplay(DisplayConfig config) {
        super(DisplayPosition.ABOVE_HEAD, config);
    }

    public boolean loadOrRender(EntityPlayer player) {
        if (player == null) {
            return false;
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            //#if MC<=10809
            if (effect.getPotionID() == 14) {
                return false;
            }
            //#else
            //$$ if (effect.getPotion().getName().equalsIgnoreCase("invisibility")) {
            //$$     return false;
            //$$ }
            //#endif
        }
        if (!renderFromTeam(player)) {
            return false;
        }
        //#if MC<=10809
        if(player.riddenByEntity != null) {
            return false;
        }
        //#else
        //$$ if(player.isBeingRidden()) {
        //$$     return false;
        //$$ }
        //#endif

        int renderDistance = Levelhead.INSTANCE.getDisplayManager().getMasterConfig().getRenderDistance();
        int min = Math.min(4096, renderDistance * renderDistance);


        return !(player.getDistanceSqToEntity(Minecraft.getMinecraft().thePlayer) > min)
            && (!player.hasCustomName() || !player.getCustomNameTag().isEmpty())
            && !player.getDisplayNameString().isEmpty()
            && existedMoreThan5Seconds.contains(player.getUniqueID())
            && !player.getDisplayName().getFormattedText().contains(LevelheadMainGUI.COLOR_CHAR + "k")
            && !player.isInvisible()
            && !player.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer)
            && !player.isSneaking();
    }

    protected boolean renderFromTeam(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().thePlayer) return true;
        Team team = player.getTeam();
        Team team1 = Minecraft.getMinecraft().thePlayer.getTeam();

        if (team != null) {
            Team.EnumVisible enumVisible = team.getNameTagVisibility();
            switch (enumVisible) {
                case NEVER:
                    return false;
                case HIDE_FOR_OTHER_TEAMS:
                    return team1 == null || team.isSameTeam(team1);
                case HIDE_FOR_OWN_TEAM:
                    return team1 == null || !team.isSameTeam(team1);
                case ALWAYS:
                default:
                    return true;
            }
        }
        return true;
    }

    @Override
    public void tick() {
        for (EntityPlayer entityPlayer : Minecraft.getMinecraft().theWorld.playerEntities) {
            if (!existedMoreThan5Seconds.contains(entityPlayer.getUniqueID())) {
                if (!timeCheck.containsKey(entityPlayer.getUniqueID()))
                    timeCheck.put(entityPlayer.getUniqueID(), 0);
                int old = timeCheck.get(entityPlayer.getUniqueID());
                if (old > 100) {
                    existedMoreThan5Seconds.add(entityPlayer.getUniqueID());
                } else if (!entityPlayer.isInvisibleToPlayer(Minecraft.getMinecraft().thePlayer))
                    timeCheck.put(entityPlayer.getUniqueID(), old + 1);
            }

            if (loadOrRender(entityPlayer)) {
                final UUID uuid = entityPlayer.getUniqueID();
                if (!cache.containsKey(uuid)) {
                    Levelhead.INSTANCE.fetch(uuid, this, bottomValue);
                }
            }
        }
    }

    public void setBottomValue(boolean bottomValue) {
        this.bottomValue = bottomValue;
    }

    private int index;

    @Override
    public void checkCacheSize() {
        int max = Math.max(150, Levelhead.INSTANCE.getDisplayManager().getMasterConfig().getPurgeSize());
        if (cache.size() > max) {
            Set<UUID> safePlayers = new HashSet<>();
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (existedMoreThan5Seconds.contains(player.getUniqueID())) {
                    safePlayers.add(player.getUniqueID());
                }
            }

            existedMoreThan5Seconds.clear();
            existedMoreThan5Seconds.addAll(safePlayers);

            for (UUID uuid : cache.keySet()) {
                if (!safePlayers.contains(uuid)) {
                    cache.remove(uuid);
                    trueValueCache.remove(uuid);
                }
            }
        }
    }

    @Override
    public void onDelete() {
        cache.clear();
        trueValueCache.clear();
        existedMoreThan5Seconds.clear();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
