/*
 *  This file is a part of project QuickShop, the name is BukkitEventManager.java
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

package com.ghostchu.quickshop.eventmanager;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.eventmanager.QuickEventManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * A simple impl for Bukkit original EventManager
 *
 * @author Ghost_chu
 */
public class BukkitEventManager implements QuickEventManager {
    @Override
    public void callEvent(@NotNull Event event) {
        QuickShop.getInstance().getServer().getPluginManager().callEvent(event);
    }
}
