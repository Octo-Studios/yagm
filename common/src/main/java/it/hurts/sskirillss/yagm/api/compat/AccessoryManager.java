package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public final class AccessoryManager {

    private static final Map<String, IAccessoryHandler> handlers = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    private AccessoryManager() {}


    public static void registerHandler(IAccessoryHandler handler) {
        if (handler == null) return;

        if (handler.isModLoaded()) {
            handlers.put(handler.getModName(), handler);
            YAGMCommon.LOGGER.info("[YAGM] Registered accessory handler: {}", handler.getModName());
        } else {
            YAGMCommon.LOGGER.debug("[YAGM] Skipped unavailable handler: {}", handler.getModName());
        }
    }

    public static void initialize() {
        if (initialized) return;
        initialized = true;
    }


    public static Collection<IAccessoryHandler> getHandlers() {
        return Collections.unmodifiableCollection(handlers.values());
    }

    public static IAccessoryHandler getHandler(String name) {
        return handlers.get(name);
    }

    public static boolean hasAnyHandler() {
        return !handlers.isEmpty();
    }

    /**
     * Collect all accessory items from player using all handlers.
     *
     * @param player The player
     * @return Map: handler name -> (slot key -> item)
     */
    public static Map<String, Map<String, ItemStack>> collectAllAccessories(ServerPlayer player) {
        Map<String, Map<String, ItemStack>> allAccessories = new HashMap<>();

        for (IAccessoryHandler handler : handlers.values()) {
            Map<String, ItemStack> accessories = handler.collectAccessories(player);
            if (!accessories.isEmpty()) {
                allAccessories.put(handler.getModName(), accessories);
            }
        }

        return allAccessories;
    }


    public static void clearAllAccessories(ServerPlayer player) {
        for (IAccessoryHandler handler : handlers.values()) {
            handler.clearAccessories(player);
        }
    }


    public static NonNullList<ItemStack> toFlatList(Map<String, Map<String, ItemStack>> allAccessories) {
        NonNullList<ItemStack> list = NonNullList.create();

        for (Map<String, ItemStack> handlerAccessories : allAccessories.values()) {
            for (ItemStack stack : handlerAccessories.values()) {
                if (!stack.isEmpty()) {
                    list.add(stack.copy());
                }
            }
        }

        return list;
    }

    /**
     * Save all accessory data to NBT.
     *
     * @param allAccessories Map from collectAllAccessories
     * @param registryAccess Registry access
     * @return NBT with data from all handlers
     */
    public static CompoundTag saveAllToNBT(Map<String, Map<String, ItemStack>> allAccessories, RegistryAccess registryAccess) {
        CompoundTag root = new CompoundTag();

        for (Map.Entry<String, Map<String, ItemStack>> entry : allAccessories.entrySet()) {
            String handlerName = entry.getKey();
            Map<String, ItemStack> accessories = entry.getValue();

            IAccessoryHandler handler = handlers.get(handlerName);
            if (handler != null) {
                CompoundTag handlerData = handler.saveToNBT(accessories, registryAccess);
                if (handlerData != null && !handlerData.isEmpty()) {
                    root.put(handlerName, handlerData);

                }
            }
        }
        return root;
    }

    /**
     * Load all accessory data from NBT.
     *
     * @param tag NBT with handler data
     * @param registryAccess Registry access
     * @return Map: handler name -> (slot key -> item)
     */
    public static Map<String, Map<String, ItemStack>> loadAllFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, Map<String, ItemStack>> allAccessories = new HashMap<>();

        for (String handlerName : tag.getAllKeys()) {
            IAccessoryHandler handler = handlers.get(handlerName);
            if (handler != null) {
                CompoundTag handlerData = tag.getCompound(handlerName);
                Map<String, ItemStack> accessories = handler.loadFromNBT(handlerData, registryAccess);
                if (!accessories.isEmpty()) {
                    allAccessories.put(handlerName, accessories);
                }
            }
        }
        return allAccessories;
    }

    /**
     * Restore all accessories to player.
     *
     * @param player The player
     * @param allAccessories Map from loadAllFromNBT
     * @param dropIfFull Whether to drop items that don't fit
     */
    public static void restoreAllAccessories(ServerPlayer player, Map<String, Map<String, ItemStack>> allAccessories, boolean dropIfFull) {
        for (Map.Entry<String, Map<String, ItemStack>> entry : allAccessories.entrySet()) {
            String handlerName = entry.getKey();
            Map<String, ItemStack> accessories = entry.getValue();

            IAccessoryHandler handler = handlers.get(handlerName);
            if (handler != null) {
                handler.restoreAccessories(player, accessories, dropIfFull);
            } else {
                for (ItemStack stack : accessories.values()) {
                    if (!stack.isEmpty()) {
                        if (!player.getInventory().add(stack.copy())) {
                            if (dropIfFull) {
                                player.drop(stack.copy(), false);
                            }
                        }
                    }
                }
            }
        }
    }
}
