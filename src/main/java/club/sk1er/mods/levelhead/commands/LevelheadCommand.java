package club.sk1er.mods.levelhead.commands;

import club.sk1er.mods.core.ModCore;
import club.sk1er.mods.core.commands.api.DefaultHandler;
import club.sk1er.mods.core.commands.api.ModCoreCommand;
import club.sk1er.mods.core.commands.api.SubCommand;
import club.sk1er.mods.core.gui.studio.CosmeticStudio;
import club.sk1er.mods.core.universal.ChatColor;
import club.sk1er.mods.core.util.GuiUtil;
import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Mitchell Katz on 5/8/2017.
 */
public class LevelheadCommand extends ModCoreCommand {

    public LevelheadCommand() {
        super("levelhead");
    }


    @DefaultHandler
    public void handle() {
        GuiUtil.open(new LevelheadMainGUI());
    }

    @SubCommand(name = "limit")
    public void handleLimit() {
        MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());
        MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + MinecraftUtils.isHypixel());
    }

    @SubCommand(name = "dumpcache")
    public void handleDumpCache() {
        Levelhead.getInstance().getDisplayManager().clearCache();
        MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
    }
}
