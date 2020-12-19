package club.sk1er.mods.levelhead.commands;


import club.sk1er.mods.core.universal.ChatColor;
import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import net.modcore.api.ModCoreAPI;
import net.modcore.api.commands.Command;
import net.modcore.api.commands.DefaultHandler;
import net.modcore.api.commands.SubCommand;
import net.modcore.api.utils.GuiUtil;

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
        ModCoreAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.getInstance().getTypes());
        ModCoreAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + ModCoreAPI.getMinecraftUtil().isHypixel());
    }

    @SubCommand(value = "dumpcache")
    public void handleDumpCache() {
        Levelhead.getInstance().getDisplayManager().clearCache();
        ModCoreAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
    }
}
