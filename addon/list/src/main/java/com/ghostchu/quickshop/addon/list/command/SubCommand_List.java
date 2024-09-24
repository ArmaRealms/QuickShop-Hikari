package com.ghostchu.quickshop.addon.list.command;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.api.command.CommandParser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.util.ChatSheetPrinter;
import com.ghostchu.quickshop.util.MsgUtil;
import com.ghostchu.quickshop.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.ghostchu.quickshop.util.Util.getPlayerList;

public class SubCommand_List implements CommandHandler<Player> {
    private final QuickShop plugin;

    private final int pageSize = 10;

    public SubCommand_List(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        int page = 1;
        if (parser.getArgs().isEmpty()) {
            lookupSelf(sender, page);
            return;
        }
        if (!StringUtils.isNumeric(parser.getArgs().get(0))) {
            if (parser.getArgs().size() >= 2) {
                if (!StringUtils.isNumeric(parser.getArgs().get(1))) {
                    plugin.text().of(sender, "not-a-number", parser.getArgs().get(1)).send();
                    return;
                }
                page = Integer.parseInt(parser.getArgs().get(2));
            }
            lookupOther(sender, parser.getArgs().get(0), page);
        } else {
            page = Integer.parseInt(parser.getArgs().get(0));
            lookupSelf(sender, page);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        if (parser.getArgs().size() == 1) {
            if (plugin.perm().hasPermission(sender, "quickshopaddon.list.other")) {
                return getPlayerList();
            }
        }
        if (parser.getArgs().size() == 2) {
            return List.of("[<page>]");
        }
        return Collections.emptyList();
    }

    private void lookupSelf(Player sender, int page) {
        if (!plugin.perm().hasPermission(sender, "quickshopaddon.list.self")) {
            plugin.text().of(sender, "no-permission").send();
            return;
        }
        lookup(sender, sender.getUniqueId(), page);
    }

    private void lookupOther(@NotNull Player sender, @NotNull String userName, int page) {
        if (!plugin.perm().hasPermission(sender, "quickshopaddon.list.other")) {
            plugin.text().of(sender, "no-permission").send();
            return;
        }
        UUID targetUser = plugin.getPlayerFinder().name2Uuid(userName);
        lookup(sender, targetUser, page);
    }

    private void lookup(@NotNull Player sender, @NotNull UUID lookupUser, int page) {
        String name = plugin.getPlayerFinder().uuid2Name(lookupUser);
        if (StringUtils.isEmpty(name)) {
            name = "Unknown";
        }
        List<Shop> shops = plugin.getShopManager().getAllShops(lookupUser);
        ChatSheetPrinter printer = new ChatSheetPrinter(sender);

        int startPos = (page - 1) * pageSize;
        int counter = 0;
        int loopCounter = 0;
        printer.printHeader();
        printer.printLine(plugin.text().of(sender, "addon.list.table-prefix-pageable", name, page, (int)Math.ceil((double)shops.size()/pageSize)).forLocale());
        for (Shop shop : shops) {
            counter++;
            if(counter < startPos) {
                continue;
            }
            String shopName = shop.getShopName();
            Location location = shop.getLocation();
            String combineLocation = location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
            if (StringUtils.isEmpty(shopName)) {
                shopName = combineLocation;
            }
            Component shopNameComponent = LegacyComponentSerializer.legacySection().deserialize(shopName).append(Component.textOfChildren(Component.text(" (").append(Util.getItemStackName(shop.getItem())).append(Component.text(")"))).color(NamedTextColor.GRAY));
            Component shopTypeComponent;
            if (shop.isBuying()) {
                shopTypeComponent = plugin.text().of(sender, "menu.this-shop-is-buying").forLocale();
            } else {
                shopTypeComponent = plugin.text().of(sender, "menu.this-shop-is-selling").forLocale();
            }
            Component component = plugin.text().of(sender, "addon.list.entry", counter, shopNameComponent, location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ(), plugin.getEconomy().format(shop.getPrice(), shop.getLocation().getWorld(), shop.getCurrency()), shop.getShopStackingAmount(), Util.getItemStackName(shop.getItem()), shopTypeComponent).forLocale();
            component = component.clickEvent(ClickEvent.runCommand(MsgUtil.fillArgs("/{0} {1} {2}", plugin.getMainCommand(), plugin.getCommandPrefix("silentpreview"), shop.getRuntimeRandomUniqueId().toString())));
            printer.printLine(component);
            loopCounter ++;
            if(loopCounter >= pageSize){
                break;
            }
        }
        printer.printFooter();

    }
}
