package club.sk1er.mods.levelhead.commands;

import club.sk1er.mods.levelhead.LevelHead;
import club.sk1er.mods.levelhead.utils.ChatColor;
import club.sk1er.mods.levelhead.utils.Sk1erMod;
import club.sk1er.mods.levelhead.guis.LevelHeadGui;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

/**
 * Created by Mitchell Katz on 5/8/2017.
 */
public class ToggleCommand extends CommandBase {

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
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("limit")) {
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Count: " + LevelHead.count);
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Wait: " + LevelHead.wait);
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Hypixel: " + Sk1erMod.getInstance().isHypixel());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Remote Status: " + Sk1erMod.getInstance().isEnabled());
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Local Stats: " + LevelHead.TOGGLED_ON);

                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Primary Color: " + LevelHead.PRIMARY_COLOR +"@");
                Sk1erMod.getInstance().sendMessage(ChatColor.RED + "Secondary Color: " + LevelHead.SECOND_COLOR +"@");
                return;
            } else if (args[0].equalsIgnoreCase("dumpcache")) {
                LevelHead.stringCache.clear();

                Sk1erMod.getInstance().sendMessage(ChatColor.translateAlternateColorCodes('&', String.format("&fStringcache entries: &b%s", LevelHead.stringCache.size())));
                return;
            }
        }
        new LevelHeadGui().display();
    }
}
