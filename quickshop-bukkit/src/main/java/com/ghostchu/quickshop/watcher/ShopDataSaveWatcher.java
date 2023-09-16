package com.ghostchu.quickshop.watcher;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.common.util.QuickExecutor;
import com.ghostchu.quickshop.util.logger.Log;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;

public class ShopDataSaveWatcher extends BukkitRunnable {
    private final QuickShop plugin;
    private CompletableFuture<Void> saveTask;

    public ShopDataSaveWatcher(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (saveTask != null && !saveTask.isDone()) {
            Log.debug("Another save task still running!");
            return;
        }
        CompletableFuture<?>[] shopsToSaveFuture = plugin.getShopManager().getAllShops().stream().filter(Shop::isDirty)
                .map(Shop::update)
                .toArray(CompletableFuture[]::new);
        saveTask = CompletableFuture.allOf(shopsToSaveFuture)
                .thenAcceptAsync((v) -> {
                    if (shopsToSaveFuture.length != 0) {
                        Log.debug("Saved " + shopsToSaveFuture.length + " shops in background.");
                    }
                }, QuickExecutor.getShopSaveExecutor())
                .exceptionally(e -> {
                    plugin.logger().warn("Error while saving shops, all failed shops will attempt save again in next time", e);
                    return null;
                });
    }
}
