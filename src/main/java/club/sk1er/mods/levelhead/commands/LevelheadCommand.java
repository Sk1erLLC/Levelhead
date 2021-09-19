package club.sk1er.mods.levelhead.commands;


import club.sk1er.mods.levelhead.Levelhead;
import club.sk1er.mods.levelhead.guis.CustomLevelheadConfigurer;
import club.sk1er.mods.levelhead.guis.LevelheadMainGUI;
import gg.essential.api.EssentialAPI;
import gg.essential.api.commands.Command;
import gg.essential.api.commands.DefaultHandler;
import gg.essential.api.commands.SubCommand;
import gg.essential.api.utils.GuiUtil;
import gg.essential.universal.ChatColor;

public class LevelheadCommand extends Command {

    public LevelheadCommand() {
        super("levelhead");
    }

    @DefaultHandler
    public void handle() {
        GuiUtil.open(new club.sk1er.mods.levelhead.gui.LevelheadMainGUI());
    }

    @SubCommand(value = "legacy")
    public void a() {GuiUtil.open(new LevelheadMainGUI());}

    @SubCommand(value = "test")
    public void b() {GuiUtil.open(new CustomLevelheadConfigurer());}

    @SubCommand(value = "limit")
    public void handleLimit() {
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Callback_types: " + Levelhead.INSTANCE.getTypes());
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Hypixel: " + EssentialAPI.getMinecraftUtil().isHypixel());
    }

    @SubCommand(value = "dumpcache")
    public void handleDumpCache() {
        Levelhead.INSTANCE.getDisplayManager().clearCache();
        EssentialAPI.getMinecraftUtil().sendMessage(Levelhead.CHAT_PREFIX, ChatColor.RED + "Cleared Cache");
    }
}
