package club.sk1er.mods.levelhead.commands;

import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelHeadGui;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
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
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Count: " + Levelhead.getInstance().count);
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Wait: " + Levelhead.getInstance().wait);
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Hypixel: " + Sk1erMod.getInstance().isHypixel());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Remote Status: " + Sk1erMod.getInstance().isEnabled());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Local Stats: " + Levelhead.getInstance().getSk1erMod().isHypixel());
//                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Header State: " + Levelhead.getInstance().getHeaderConfig());
//                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Footer State: " + Levelhead.getInstance().getFooterConfig());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Callback: " + Sk1erMod.getInstance().getResponse());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());

                return;
            } else if (args[0].equalsIgnoreCase("dumpcache")) {
                Levelhead.getInstance().getDisplayManager().clearCache();
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Cleared Cache");
                return;
            }
        }
        new LevelHeadGui().display();
    }
}
