/*
 *  This file is a part of project QuickShop, the name is CompatibilityManager.java
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

package com.ghostchu.quickshop.api.accompatibility;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Manager that managing all registered compatibility for anti-cheat modules
 */
@Deprecated(forRemoval = true)
public interface AntiCheatCompatibilityManager {
    /**
     * Check a module registered
     *
     * @param moduleName Module name
     * @return Is registered
     */
    @Deprecated(forRemoval = true)
    boolean isRegistered(String moduleName);

    /**
     * Switch the compatibility mode on or off, set false to disable all we known incompatiable plugin
     * listener, set true to enable back all disabled plugin liseners. WIP
     *
     * @param status true=turn on closed listeners, false=turn off all turned on listeners.
     * @param player The player to check the listeners
     */
    @Deprecated(forRemoval = true)
    void toggleProtectionListeners(boolean status, @NotNull Player player);

    /**
     * Unregister all registered compatibility modules
     */
    @Deprecated(forRemoval = true)
    void unregisterAll();

    /**
     * Register compatibility module
     *
     * @param module Compatibility module
     */
    @Deprecated(forRemoval = true)
    void register(@NotNull AntiCheatCompatibilityModule module);

    /**
     * Unregister a registered compatibility modules
     *
     * @param moduleName Compatibility module name
     */
    @Deprecated(forRemoval = true)
    void unregister(@NotNull String moduleName);

    /**
     * Unregister a registered compatibility modules
     *
     * @param module Compatibility module
     */
    @Deprecated(forRemoval = true)
    void unregister(@NotNull AntiCheatCompatibilityModule module);
}
