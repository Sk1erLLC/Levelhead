package club.sk1er.mods.levelhead.commands;

import club.sk1er.mods.core.ModCore;
import club.sk1er.mods.core.command.ModCoreCommand;
import club.sk1er.mods.core.universal.ChatColor;
import club.sk1er.mods.core.util.GuiUtil;
import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

/**
 * Created by Mitchell Katz on 5/8/2017.
 */
public class LevelheadCommand extends ModCoreCommand {

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    public String getCommandName() {
        return "levelhead";
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("limit")) {
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + MinecraftUtils.isHypixel());
                return;
            } else if (args[0].equalsIgnoreCase("dumpcache")) {
                Levelhead.getInstance().getDisplayManager().clearCache();
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
                return;
            }
        }

        GuiUtil.open(new LevelheadMainGUI());
    }
}
