package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class AccessoryManager {

    private static final Map<String, IAccessoryHandler> handlers = new ConcurrentHashMap<>();
    private static boolean initialized = false;

    public static void registerHandler(IAccessoryHandler handler) {
        if (handler == null) return;

        if (handler.isModLoaded()) {
            handlers.put(handler.getModName(), handler);
            log.info("[YAGM] Registered accessory handler: {}", handler.getModName());
        } else {
            log.debug("[YAGM] Skipped unavailable handler: {}", handler.getModName());
        }
    }

    public static void init () {
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
