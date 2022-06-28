/*
 *  This file is a part of project QuickShop, the name is SubCommand_Help.java
 *  Copyright (C) Ghost_chu and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.ghostchu.quickshop.command.subcommand;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandContainer;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.util.MsgUtil;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AllArgsConstructor
public class SubCommand_Help implements CommandHandler<CommandSender> {

    private final QuickShop plugin;

    @Override
    public void onCommand(
            @NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] cmdArg) {
        sendHelp(sender, commandLabel);
    }


    private void sendHelp(@NotNull CommandSender s, @NotNull String commandLabel) {
        plugin.text().of(s, "command.description.title").send();
        String locale = MsgUtil.getDefaultGameLanguageCode();
        if (s instanceof Player p) {
            locale = p.getLocale();
        }
        commandPrintingLoop:
        for (CommandContainer container : plugin.getCommandManager().getRegisteredCommands()) {
            if (!container.isHidden()) {
                boolean passed = false;
                //selectivePermissions
                final List<String> selectivePermissions = container.getSelectivePermissions();
                if (selectivePermissions != null && !selectivePermissions.isEmpty()) {
                    for (String selectivePermission : container.getSelectivePermissions()) {
                        if (selectivePermission != null && !selectivePermission.isEmpty()) {
                            if (plugin.perm().hasPermission(s, selectivePermission)) {
                                passed = true;
                                break;
                            }
                        }
                    }
                }
                //requirePermissions
                final List<String> requirePermissions = container.getPermissions();
                if (requirePermissions != null && !requirePermissions.isEmpty()) {
                    for (String requirePermission : requirePermissions) {
                        if (requirePermission != null && !requirePermission.isEmpty() && !plugin.perm().hasPermission(s, requirePermission)) {
                            continue commandPrintingLoop;
                        }
                    }
                    passed = true;
                }
                if (!passed) {
                    continue;
                }
                Component commandDesc = plugin.text().of(s, "command.description." + container.getPrefix()).forLocale();
                if (container.getDescription() != null) {
                    commandDesc = container.getDescription().apply(locale);
                    if (commandDesc == null) {
                        commandDesc = Component.text("Error: Subcommand " + container.getPrefix() + " # " + container.getClass().getCanonicalName() + " doesn't register the correct help description.");
                    }
                }
                if (container.isDisabled() || (container.getDisabledSupplier() != null && container.getDisabledSupplier().get())) {
                    if (plugin.perm().hasPermission(s, "quickshop.showdisabled")) {
                        plugin.text().of(s, "command.format-disabled", commandLabel, container.getPrefix(), container.getDisableText(s)).send();
                    }
                } else {
                    plugin.text().of(s, "command.format", commandLabel, container.getPrefix(), commandDesc).send();
                }
            }
        }
    }

}
