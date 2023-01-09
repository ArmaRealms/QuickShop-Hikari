package com.ghostchu.quickshop.shop.display;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.GameVersion;
import com.ghostchu.quickshop.api.event.ShopDisplayItemSpawnEvent;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.display.DisplayType;
import com.ghostchu.quickshop.shop.ContainerShop;
import com.ghostchu.quickshop.shop.SimpleShopChunk;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import com.ghostchu.simplereloadlib.Reloadable;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualDisplayItem extends AbstractDisplayItem implements Reloadable {
    private static final AtomicInteger COUNTER = new AtomicInteger(Integer.MAX_VALUE);
    private static final GameVersion VERSION = QuickShop.getInstance().getGameVersion();
    private static final ProtocolManager PROTOCOL_MANAGER = ProtocolLibrary.getProtocolManager();
    private static PacketAdapter packetAdapter = null;
    //unique EntityID
    private final int entityID = COUNTER.decrementAndGet();
    //The List which store packet sender
    private final Set<UUID> packetSenders = new ConcurrentSkipListSet<>();
    //cache chunk x and z
    private SimpleShopChunk chunkLocation;
    private volatile boolean isDisplay;
    //If packet initialized
    private volatile boolean initialized = false;
    //packets
    private PacketContainer fakeItemSpawnPacket;
    private PacketContainer fakeItemMetaPacket;
    private PacketContainer fakeItemVelocityPacket;
    private PacketContainer fakeItemDestroyPacket;

    public VirtualDisplayItem(@NotNull Shop shop) throws RuntimeException {
        super(shop);
        VirtualDisplayItemManager.load();

    }

    @Override
    public boolean checkDisplayIsMoved() {
        return false;
    }

    @Override
    public boolean checkDisplayNeedRegen() {
        return false;
    }

    @Override
    public boolean checkIsShopEntity(@NotNull Entity entity) {
        return false;
    }

    @Override
    public void fixDisplayMoved() {

    }

    @Override
    public void fixDisplayNeedRegen() {

    }

    @Override
    public @Nullable Entity getDisplay() {
        return null;
    }

    @Override
    public boolean isSpawned() {
        if (shop.isLeftShop()) {
            Shop aShop = shop.getAttachedShop();
            if (aShop instanceof ContainerShop) {
                return (Objects.requireNonNull(((ContainerShop) aShop).getDisplayItem())).isSpawned();
            }

        }
        return isDisplay;
    }

    @Override
    public void remove() {
        if (isDisplay) {
            sendPacketToAll(fakeItemDestroyPacket);
            unload();
            isDisplay = false;
        }
    }

    @Override
    public boolean removeDupe() {
        return false;
    }

    @Override
    public void respawn() {
        Util.ensureThread(false);
        remove();
        spawn();
    }

    @Override
    public void safeGuard(@Nullable Entity entity) {

    }

    @Override
    public void spawn() {
        Util.ensureThread(false);
        if (shop.isLeftShop() || isDisplay || shop.isDeleted() || !shop.isLoaded()) {
            return;
        }
        if (new ShopDisplayItemSpawnEvent(shop, originalItemStack, DisplayType.VIRTUALITEM).callCancellableEvent()) {
            Log.debug(
                    "Canceled the displayItem spawning because a plugin setCancelled the spawning event, usually this is a QuickShop Add on");
            return;
        }

        //lazy initialize
        if (!initialized) {
            initFakeDropItemPacket();
        }

        load();

        // Can't rely on the attachedShop cache to be accurate
        // So just try it and if it fails, no biggie
        /*try {
            shop.getAttachedShop().updateAttachedShop();
        } catch (NullPointerException ignored) {
        }*/

        sendFakeItemToAll();
        isDisplay = true;
    }

    private void initFakeDropItemPacket() {
        fakeItemSpawnPacket = PacketFactory.createFakeItemSpawnPacket(entityID, getDisplayLocation());
        fakeItemMetaPacket = PacketFactory.createFakeItemMetaPacket(entityID, getOriginalItemStack().clone());
        fakeItemVelocityPacket = PacketFactory.createFakeItemVelocityPacket(entityID);
        fakeItemDestroyPacket = PacketFactory.createFakeItemDestroyPacket(entityID);
        initialized = true;
    }

    @Override
    public ReloadResult reloadModule() {
        init();
        return new ReloadResult(ReloadStatus.SUCCESS, "OK", null);
    }

    //Due to the delay task in ChunkListener
    //We must move load task to first spawn to prevent some bug and make the check lesser
    private void load() {
        Util.ensureThread(false);
        //some time shop can be loaded when world isn't loaded
        Chunk chunk = shop.getLocation().getChunk();
        chunkLocation = new SimpleShopChunk(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        VirtualDisplayItemManager.put(chunkLocation, this);
        if (Util.isLoaded(shop.getLocation())) {
            //Let nearby player can saw fake item
            Collection<Entity> entityCollection = shop.getLocation().getWorld().getNearbyEntities(shop.getLocation(), Bukkit.getViewDistance() * 16, shop.getLocation().getWorld().getMaxHeight(), Bukkit.getViewDistance() * 16);
            for (Entity entity : entityCollection) {
                if (entity instanceof Player) {
                    packetSenders.add(entity.getUniqueId());
                }
            }
        }
    }

    public void sendFakeItem(@NotNull Player player) {
        sendPacket(player, fakeItemSpawnPacket);
        sendPacket(player, fakeItemMetaPacket);
        sendPacket(player, fakeItemVelocityPacket);
    }

    private void sendPacket(@NotNull Player player, @NotNull PacketContainer packet) {
        PROTOCOL_MANAGER.sendServerPacket(player, packet);
    }

    public void sendFakeItemToAll() {
        sendPacketToAll(fakeItemSpawnPacket);
        sendPacketToAll(fakeItemMetaPacket);
        sendPacketToAll(fakeItemVelocityPacket);
    }

    private void sendPacketToAll(@NotNull PacketContainer packet) {
        Iterator<UUID> iterator = packetSenders.iterator();
        while (iterator.hasNext()) {
            Player nextPlayer = Bukkit.getPlayer(iterator.next());
            if (nextPlayer == null) {
                iterator.remove();
            } else {
                sendPacket(nextPlayer, packet);
            }
        }
    }

    private void unload() {
        packetSenders.clear();
        VirtualDisplayItemManager.remove(chunkLocation, this);
    }

    public static class VirtualDisplayItemManager {
        private static final AtomicBoolean LOADED = new AtomicBoolean(false);
        private static final Map<SimpleShopChunk, List<VirtualDisplayItem>> CHUNKS_MAPPING = new ConcurrentHashMap<>();

        private VirtualDisplayItemManager() {
        }

        public static void load() {
            if (LOADED.get()) {
                return;
            }
            String stringClassLoader = PROTOCOL_MANAGER.getClass().getClassLoader().toString();
            if(stringClassLoader.contains("pluginEnabled=true")&&!stringClassLoader.contains("plugin=ProtocolLib")) {
                PLUGIN.logger().warn("Warning! ProtocolLib seems provided by another plugin, This seems to be a wrong packaging problem, " +
                        "QuickShop can't ensure the ProtocolLib is working correctly! Info: {}", stringClassLoader);
            }
            Log.debug("Loading VirtualDisplayItem chunks mapping manager...");
            if (packetAdapter == null) {
                packetAdapter = new PacketAdapter(PLUGIN.getJavaPlugin(), ListenerPriority.HIGH, PacketType.Play.Server.MAP_CHUNK) {
                    @Override
                    public void onPacketSending(@NotNull PacketEvent event) {
                        //is really full chunk data
                        //In 1.17, this value was removed, so read safely
                        Boolean boxedIsFull = event.getPacket().getBooleans().readSafely(0);
                        boolean isFull = boxedIsFull == null || boxedIsFull;
                        if (!isFull) {
                            return;
                        }
                        Player player = event.getPlayer();
                        if (player == null || !player.isOnline()) {
                            return;
                        }
                        if (player.getClass().getName().contains("TemporaryPlayer")) {
                            return;
                        }
                        StructureModifier<Integer> integerStructureModifier = event.getPacket().getIntegers();
                        //chunk x
                        int x = integerStructureModifier.read(0);
                        //chunk z
                        int z = integerStructureModifier.read(1);

                        CHUNKS_MAPPING.computeIfPresent(new SimpleShopChunk(player.getWorld().getName(), x, z), (chunkLoc, targetList) -> {
                            for (VirtualDisplayItem target : targetList) {
                                if (!target.shop.isLoaded() || !target.isDisplay || target.shop.isLeftShop()) {
                                    continue;
                                }
                                target.packetSenders.add(player.getUniqueId());
                                target.sendFakeItem(player);
                            }
                            return targetList;
                        });
                    }
                };
                Log.debug("Registering the packet listener...");
                PROTOCOL_MANAGER.addPacketListener(packetAdapter);
                LOADED.set(true);
            }
        }

        public static void put(@NotNull SimpleShopChunk key, @NotNull VirtualDisplayItem value) {
            //Thread-safe was ensured by ONLY USE Map method to do something
            List<VirtualDisplayItem> virtualDisplayItems = new ArrayList<>(Collections.singletonList(value));
            CHUNKS_MAPPING.merge(key, virtualDisplayItems, (mapOldVal, mapNewVal) -> {
                mapOldVal.addAll(mapNewVal);
                return mapOldVal;
            });
        }

        public static void remove(@NotNull SimpleShopChunk key, @NotNull VirtualDisplayItem value) {
            CHUNKS_MAPPING.computeIfPresent(key, (mapOldKey, mapOldVal) -> {
                mapOldVal.remove(value);
                return mapOldVal;
            });
        }

        public static void unload() {
            Log.debug("Unloading VirtualDisplayItem chunks mapping manager...");
            if (LOADED.get()) {
                Log.debug("Unregistering the packet listener...");
                PROTOCOL_MANAGER.removePacketListener(packetAdapter);
                LOADED.set(false);
            }
        }
    }

    public static class PacketFactory {
        private PacketFactory() {
        }

        public static Throwable testFakeItem() {
            try {
                createFakeItemSpawnPacket(0, new Location(Bukkit.getWorlds().get(0), 0, 0, 0));
                createFakeItemMetaPacket(0, new ItemStack(Material.values()[0]));
                createFakeItemVelocityPacket(0);
                createFakeItemDestroyPacket(0);
                return null;
            } catch (Exception throwable) {
                return throwable;
            }
        }

        private static PacketContainer createFakeItemSpawnPacket(int entityID, Location displayLocation) {
            //First, create a new packet to spawn item
            PacketContainer fakeItemPacket = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SPAWN_ENTITY);
            //and add data based on packet class in NMS  (global scope variable)
            //Reference: https://wiki.vg/Protocol#Spawn_Object
            fakeItemPacket.getIntegers()
                    //Entity ID
                    .write(0, entityID);
            //Velocity x
            switch (VERSION) {
                //int data to mark
                case v1_18_R1, v1_18_R2 -> {
                    fakeItemPacket.getEntityTypeModifier().write(0, EntityType.DROPPED_ITEM);
                    fakeItemPacket.getIntegers().write(1, 0)
                            //Velocity y
                            .write(2, 0)
                            //Velocity z
                            .write(3, 0)
                            //Pitch
                            .write(4, 0)
                            //Yaw
                            .write(5, 0);
                    //int data to mark
                    fakeItemPacket.getIntegers().write(6, 1);
                }
                default -> //For 1.14+, we should use EntityType
                        fakeItemPacket.getEntityTypeModifier().write(0, EntityType.DROPPED_ITEM);
            }
            //UUID
            fakeItemPacket.getUUIDs().write(0, UUID.randomUUID());
            //Location
            fakeItemPacket.getDoubles()
                    //X
                    .write(0, displayLocation.getX())
                    //Y
                    .write(1, displayLocation.getY())
                    //Z
                    .write(2, displayLocation.getZ());
            return fakeItemPacket;
        }

        private static PacketContainer createFakeItemMetaPacket(int entityID, ItemStack itemStack) {
            //Next, create a new packet to update item data (default is empty)
            PacketContainer fakeItemMetaPacket = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA);
            //Entity ID
            fakeItemMetaPacket.getIntegers().write(0, entityID);

            //List<DataWatcher$Item> Type are more complex
            //Create a DataWatcher
            WrappedDataWatcher wpw = new WrappedDataWatcher();
            //https://wiki.vg/index.php?title=Entity_metadata#Entity
            if (PLUGIN.getConfig().getBoolean("shop.display-item-use-name")) {
                String itemName = GsonComponentSerializer.gson().serialize(Util.getItemStackName(itemStack));
                wpw.setObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true), Optional.of(WrappedChatComponent.fromJson(itemName).getHandle()));
                wpw.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), true);
            }

            //Must in the certain slot:https://wiki.vg/Entity_metadata#Item
            //Is 1.17-?
            if (VERSION.ordinal() < GameVersion.v1_17_R1.ordinal()) {
//                if (version == GameVersion.v1_13_R1 || version == GameVersion.v1_13_R2) {
//                    //For 1.13 is 6
//                    wpw.setObject(6, WrappedDataWatcher.Registry.getItemStackSerializer(false), itemStack);
//                } else {
                //1.14-1.16 is 7
                wpw.setObject(7, WrappedDataWatcher.Registry.getItemStackSerializer(false), itemStack);
                // }
            } else {
                //1.17+ is 8
                wpw.setObject(8, WrappedDataWatcher.Registry.getItemStackSerializer(false), itemStack);
            }
            //Add it
            //For 1.19.2+, we need to use DataValue instead of WatchableObject
            if (VERSION.ordinal() > GameVersion.v1_19_R1.ordinal()) {
                //Check for new version protocolLib
                try {
                    Class.forName("com.comphenix.protocol.wrappers.WrappedDataValue");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Unable to initialize packet, ProtocolLib update needed", e);
                }
                //Convert List<WrappedWatchableObject> to List<WrappedDataValue>
                List<WrappedWatchableObject> wrappedWatchableObjects = wpw.getWatchableObjects();
                List<WrappedDataValue> wrappedDataValues = new java.util.LinkedList<>();
                for (WrappedWatchableObject wrappedWatchableObject : wrappedWatchableObjects) {
                    WrappedDataWatcher.WrappedDataWatcherObject watchableObject = wrappedWatchableObject.getWatcherObject();
                    wrappedDataValues.add(new WrappedDataValue(watchableObject.getIndex(), watchableObject.getSerializer(), wrappedWatchableObject.getRawValue()));
                }
                fakeItemMetaPacket.getDataValueCollectionModifier().write(0, wrappedDataValues);
            } else {
                fakeItemMetaPacket.getWatchableCollectionModifier().write(0, wpw.getWatchableObjects());
            }
            return fakeItemMetaPacket;
        }

        private static PacketContainer createFakeItemVelocityPacket(int entityID) {
            //And, create a entity velocity packet to make it at a proper location (otherwise it will fly randomly)
            PacketContainer fakeItemVelocityPacket = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_VELOCITY);
            fakeItemVelocityPacket.getIntegers()
                    //Entity ID
                    .write(0, entityID)
                    //Velocity x
                    .write(1, 0)
                    //Velocity y
                    .write(2, 0)
                    //Velocity z
                    .write(3, 0);
            return fakeItemVelocityPacket;
        }

        private static PacketContainer createFakeItemDestroyPacket(int entityID) {
            //Also make a DestroyPacket to remove it
            PacketContainer fakeItemDestroyPacket = PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_DESTROY);

            // < 1.17
            if (VERSION.ordinal() < GameVersion.v1_17_R1.ordinal()) {
                fakeItemDestroyPacket.getIntegerArrays().write(0, new int[]{entityID});
                return fakeItemDestroyPacket;
            }
            // if (VERSION.ordinal() <= GameVersion.v1_19_R1.ordinal()) {
            MinecraftVersion minecraftVersion = PROTOCOL_MANAGER.getMinecraftVersion();
            if (minecraftVersion.getMajor() == 1 && minecraftVersion.getMinor() == 17 && minecraftVersion.getBuild() == 0) {
                //On 1.17, just need to write a int
                //Entity to remove
                fakeItemDestroyPacket.getIntegers().write(0, entityID);
            } else {
                //On 1.17.1 (may be 1.17.1+? it's enough, Mojang, stop the changes), we need add the int list
                //Entity to remove
                try {
                    fakeItemDestroyPacket.getIntLists().write(0, Collections.singletonList(entityID));
                } catch (NoSuchMethodError e) {
                    throw new IllegalStateException("Unable to initialize packet, ProtocolLib update needed", e);
                }
            }
            // }
            return fakeItemDestroyPacket;
        }
    }
}
