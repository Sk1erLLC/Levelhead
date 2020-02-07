package club.sk1er.mods.levelhead.commands;

import club.sk1er.mods.core.util.MinecraftUtils;
import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import club.sk1er.mods.levelhead.utils.ChatColor;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

/**
 * Created by Mitchell Katz on 5/8/2017.
 */
public class LevelheadCommand extends CommandBase {

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public String getCommandName() {
        return "levelhead";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + getCommandName();
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        //TODO update
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("limit")) {
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Count: " + Levelhead.getInstance().count);
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Wait: " + Levelhead.getInstance().wait);
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + MinecraftUtils.isHypixel());
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());
                //TODO add more debug
                return;
            } else if (args[0].equalsIgnoreCase("dumpcache")) {
                Levelhead.getInstance().getDisplayManager().clearCache();
                MinecraftUtils.sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
                return;
            }
        }
        new LevelheadMainGUI().display();
    }
}
