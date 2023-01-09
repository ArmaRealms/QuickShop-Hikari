package com.ghostchu.quickshop.command.subcommand;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.common.util.CommonUtil;
import com.ghostchu.quickshop.util.MsgUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class SubCommand_About implements CommandHandler<CommandSender> {
    private final QuickShop plugin;

    public SubCommand_About(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        MsgUtil.sendDirectMessage(sender, LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA + "QuickShop " + ChatColor.YELLOW + QuickShop.getInstance().getFork()));
        MsgUtil.sendDirectMessage(sender, LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA
                + "Version "
                + ChatColor.YELLOW
                + ">> "
                + ChatColor.GREEN
                + QuickShop.getInstance().getVersion()));
        if (plugin.getBuildInfo().getGitInfo().getBranch().toUpperCase().contains("ORIGIN/LTS")) {
            MsgUtil.sendDirectMessage(sender, LegacyComponentSerializer.legacySection().deserialize(
                    ChatColor.AQUA
                            + "Release "
                            + ChatColor.YELLOW
                            + ">> "
                            + ChatColor.GREEN
                            + LegacyComponentSerializer.legacySection().serialize(plugin.text().of(sender, "updatenotify.label.lts").forLocale())));
        } else if (plugin.getBuildInfo().getGitInfo().getBranch().toUpperCase().contains("ORIGIN/RELEASE")) {
            MsgUtil.sendDirectMessage(sender,
                    LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA
                            + "Release "
                            + ChatColor.YELLOW
                            + ">> "
                            + ChatColor.GREEN
                            + LegacyComponentSerializer.legacySection().serialize(plugin.text().of(sender, "updatenotify.label.stable").forLocale())));
        } else {
            MsgUtil.sendDirectMessage(sender,
                    LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA
                            + "Release "
                            + ChatColor.YELLOW
                            + ">> "
                            + ChatColor.GREEN
                            + LegacyComponentSerializer.legacySection().serialize(plugin.text().of(sender, "updatenotify.label.unstable").forLocale())));
        }
        MsgUtil.sendDirectMessage(sender,
                LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA
                        + "Developers "
                        + ChatColor.YELLOW
                        + ">> "
                        + ChatColor.GREEN
                        + CommonUtil.list2String(plugin.getJavaPlugin().getDescription().getAuthors())));
        MsgUtil.sendDirectMessage(sender, LegacyComponentSerializer.legacySection().deserialize(ChatColor.GOLD + "Powered by Community"));
        MsgUtil.sendDirectMessage(sender, LegacyComponentSerializer.legacySection().deserialize(ChatColor.RED + "Made with ❤"));
    }


}
