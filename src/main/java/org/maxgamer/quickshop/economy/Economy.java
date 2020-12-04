/*
 * This file is a part of project QuickShop, the name is Economy.java
 *  Copyright (C) Ghost_chu <https://github.com/Ghost-chu>
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.economy;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.util.Util;

import java.util.UUID;

public class Economy implements EconomyCore {

    private final QuickShop plugin;
    @Getter
    @Setter
    @NotNull
    private EconomyCore core;

    public Economy(@NotNull QuickShop plugin, @NotNull EconomyCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public static EconomyType getNowUsing() {
        return EconomyType.fromID(QuickShop.getInstance().getConfig().getInt("economy-type"));
    }

    @Override
    public String toString() {
        return core.getClass().getName().split("_")[1];
    }

    @Override
    public boolean deposit(@NotNull UUID name, double amount) {
        return core.deposit(name, amount);
    }

    @Override
    public boolean deposit(OfflinePlayer trader, double amount) {
        return core.deposit(trader, amount);
    }

    /**
     * Formats the given number... E.g. 50.5 becomes $50.5 Dollars, or 50 Dollars 5 Cents
     *
     * @param balance The given number
     * @return The balance in human readable text.
     */
    @Override
    public String format(double balance) {
        return Util.parseColours(core.format(balance));
        // Fix color issue from some stupid economy plugin....
    }

    @Override
    public double getBalance(@NotNull UUID name) {
        return core.getBalance(name);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return core.getBalance(player);
    }

    @Override
    public boolean transfer(@NotNull UUID from, UUID to, double amount) {
        return core.transfer(from, to, amount);
    }

    @Override
    public boolean withdraw(@NotNull UUID name, double amount) {
        return core.withdraw(name, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer trader, double amount) {
        return core.withdraw(trader, amount);
    }

    /**
     * Checks that this economy is valid. Returns false if it is not valid.
     *
     * @return True if this economy will work, false if it will not.
     */
    @Override
    public boolean isValid() {
        return core.isValid();
    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-Economy Processor";
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

}
