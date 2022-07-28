package club.sk1er.mods.levelhead.gui.components.previews

import club.sk1er.mods.levelhead.render.TabRender
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIText
import gg.essential.elementa.dsl.*
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UMinecraft
import gg.essential.universal.wrappers.UPlayer
import net.minecraft.client.gui.Gui
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraftforge.fml.client.config.GuiUtils
import java.awt.Color

class TabPreviewComponent : LevelheadPreviewComponent() {

    private val background = UIBlock(Color(Int.MIN_VALUE)).constrain {
        width = basicWidthConstraint { totalTabWidth.toFloat() }
        height = 8.pixels
    } childOf this

    private val playerInfo = UMinecraft.getNetHandler()!!.getPlayerInfo(UPlayer.getUUID())
    private val player = UPlayer.getPlayer()!!
    private val formattedName = player.displayName.formattedText
    private val totalTabWidth: Int
        get() = 9 + UMinecraft.getFontRenderer().getStringWidth(formattedName) +
                TabRender.getLevelheadWidth(playerInfo) + 15
    private val playername = UIText(formattedName).constrain {
        x = 9.pixels()
    } childOf this

    init {
        constrain {
            width = basicWidthConstraint { totalTabWidth.toFloat() }
            height = 8.pixels
        }
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDraw(matrixStack)
        super.draw(matrixStack)
        val x = this.getLeft().toInt()
        val y = this.getTop().toInt()

        UMinecraft.getMinecraft().textureManager.bindTexture(player.locationSkin)
        matrixStack.runWithGlobalState {
            Gui.drawScaledCustomSizeModalRect(
                x, y,
                8f, 8f,
                8, 8,
                8, 8,
                64f, 64f
            )
            if (player.isWearing(EnumPlayerModelParts.HAT)) {
                Gui.drawScaledCustomSizeModalRect(
                    x, y,
                    40.0f, 8f,
                    8, 8,
                    8, 8,
                    64.0f, 64.0f
                )
            }

            drawPing(x + totalTabWidth, y, playerInfo)
            TabRender.drawPingHook(0, x + totalTabWidth, y, playerInfo)
        }
    }

    private fun drawPing(x: Int, y: Int, networkPlayerInfoIn: NetworkPlayerInfo) {
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
        UMinecraft.getMinecraft().textureManager.bindTexture(Gui.icons)
        val j = if (networkPlayerInfoIn.responseTime < 0) {
            5
        } else if (networkPlayerInfoIn.responseTime < 300) {
            1
        } else if (networkPlayerInfoIn.responseTime < 600) {
            2
        } else if (networkPlayerInfoIn.responseTime < 1000) {
            3
        } else {
            4
        }
        GuiUtils.drawTexturedModalRect(x - 11, y, 0, 176 + j * 8, 10, 8, 100f)
    }

    override fun update() = Unit
}