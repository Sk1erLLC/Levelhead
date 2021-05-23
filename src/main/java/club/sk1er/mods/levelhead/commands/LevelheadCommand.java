package club.sk1er.mods.levelhead.commands;


import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import gg.essential.api.EssentialAPI;
import gg.essential.api.commands.Command;
import gg.essential.api.commands.DefaultHandler;
import gg.essential.api.commands.SubCommand;
import gg.essential.api.utils.GuiUtil;
import gg.essential.universal.ChatColor;

/**
 * Created by Mitchell Katz on 5/8/2017.
 */
public class LevelheadCommand extends Command {

    public LevelheadCommand() {
        super("levelhead");
    }


    @DefaultHandler
    public void handle() {
        GuiUtil.open(new LevelheadMainGUI());
    }

    @SubCommand(value = "limit")
    public void handleLimit() {
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + EssentialAPI.getMinecraftUtil().isHypixel());
    }

    @SubCommand(value = "dumpcache")
    public void handleDumpCache() {
        Levelhead.getInstance().getDisplayManager().clearCache();
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
    }
}
