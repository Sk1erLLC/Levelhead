package club.sk1er.mods.levelhead.display

import club.sk1er.mods.levelhead.Levelhead
import club.sk1er.mods.levelhead.config.DisplayConfig
import club.sk1er.mods.levelhead.core.isNPC
import club.sk1er.mods.levelhead.core.trimmed
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.scoreboard.Team.EnumVisible
import java.util.*
import kotlin.math.min
import kotlin.properties.Delegates

//#if MC==11202
//$$ import net.minecraft.init.MobEffects;
//#endif

class AboveHeadDisplay(config: DisplayConfig) : LevelheadDisplay(DisplayPosition.ABOVE_HEAD, config) {

    var bottomValue = true
    var index by Delegates.notNull<Int>()

    override fun loadOrRender(player: EntityPlayer?): Boolean {
        if (player == null) return false
        if (player.isPotionActive(
            //#if MC==10809
            Potion.invisibility
            //#endif
            //#if MC==11202
            //$$ MobEffects.INVISIBILITY
            //#endif
        )) return false
        if (!renderFromTeam(player)) return false
        //#if MC==10809
        if (player.riddenByEntity != null) return false
        //#else
        //$$ if (!player.getPassengers().isEmpty()) return false
        //#endif
        val min = min(4096, Levelhead.displayManager.config.renderDistance * Levelhead.displayManager.config.renderDistance)
        return player.getDistanceSqToEntity(UMinecraft.getPlayer()!!) <= min
                && (!player.hasCustomName() || player.customNameTag.isNotEmpty())
                && player.displayNameString.isNotEmpty()
                && super.loadOrRender(player)
                && !player.isInvisible
                && !player.isInvisibleToPlayer(UMinecraft.getMinecraft().thePlayer)
                && !player.isSneaking
    }

    private fun renderFromTeam(player: EntityPlayer): Boolean {
        if (player.isUser) return true
        val team = player.team
        val team1 = UPlayer.getPlayer()?.team
        if (team != null) {
            return when (team.nameTagVisibility) {
                EnumVisible.NEVER -> false
                EnumVisible.HIDE_FOR_OTHER_TEAMS -> team1 == null || team.isSameTeam(team1)
                EnumVisible.HIDE_FOR_OWN_TEAM -> team1 == null || !team.isSameTeam(team1)
                EnumVisible.ALWAYS -> true
                else -> true
            }
        }
        return true
    }

    override fun joinWorld() {
        UMinecraft.getMinecraft().theWorld.playerEntities
            .filter { !cache.containsKey(it.uniqueID) && !it.isNPC }
            .map { Levelhead.LevelheadRequest(it.uniqueID.trimmed, this, bottomValue) }
            .run { Levelhead.fetch(this) }
    }

    override fun playerJoin(player: EntityPlayer) {
        if (player.isNPC) return
        if (!cache.containsKey(player.uniqueID))
            Levelhead.fetch(listOf(Levelhead.LevelheadRequest(player.uniqueID.trimmed, this, bottomValue)))
    }

}