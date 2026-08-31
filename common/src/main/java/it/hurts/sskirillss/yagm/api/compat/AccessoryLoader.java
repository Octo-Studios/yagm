package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class AccessoryLoader {

    private static final Map<String, IAccessoryHandler> handlers = new ConcurrentHashMap<>();

    public static void registerHandler(IAccessoryHandler handler) {
        if (handler == null) return;

        if (handler.isModLoaded()) {
            handlers.put(handler.getModName(), handler);
            log.info("[YAGM] Registered accessory handler: {}", handler.getModName());
        } else {
            log.debug("[YAGM] Skipped unavailable handler: {}", handler.getModName());
        }
    }

    public static boolean hasAnyHandler() {
        return !handlers.isEmpty();
    }


    public static Map<String, Map<String, ItemStack>> collectAccessories(ServerPlayer player) {
        Map<String, Map<String, ItemStack>> allAccessories = new HashMap<>();

        for (IAccessoryHandler handler : handlers.values()) {
            Map<String, ItemStack> accessories = handler.collectAccessories(player);
            if (!accessories.isEmpty()) {
                allAccessories.put(handler.getModName(), accessories);
            }
        }

        return allAccessories;
    }


    public static void clearAccessories(ServerPlayer player) {
        for (IAccessoryHandler handler : handlers.values()) {
            handler.clearAccessories(player);
        }
    }


    public static CompoundTag saveNBT(Map<String, Map<String, ItemStack>> allAccessories, RegistryAccess registryAccess) {
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


    public static Map<String, Map<String, ItemStack>> loadNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, Map<String, ItemStack>> allAccessories = new HashMap<>();

        for (String handlerName : tag.getAllKeys()) {
            IAccessoryHandler handler = handlers.get(handlerName);

            if (handler != null) {

                CompoundTag handlerData = tag.getCompound(handlerName);

                Map<String, ItemStack> accessories = handler.loadFromNBT(handlerData, registryAccess);
                if (!accessories.isEmpty()) {
                    allAccessories.put(handlerName, accessories);
                }
            } else {
                CompoundTag handlerData = tag.getCompound(handlerName);

                Map<String, ItemStack> fallbackAccessories = parseUnknownAccessories(handlerData, registryAccess);

                if (!fallbackAccessories.isEmpty()) {
                    log.warn("[YAGM] No handler for '{}', {} items may be restored via inventory/drop fallback", handlerName, fallbackAccessories.size());
                    allAccessories.put(handlerName, fallbackAccessories);
                } else {
                    log.warn("[YAGM] No handler for '{}', unable to decode accessory payload", handlerName);
                }
            }
        }
        return allAccessories;
    }


    public static void restoreAccessories(ServerPlayer player, Map<String, Map<String, ItemStack>> allAccessories, boolean dropIfFull, BlockPos dropPos, Level level) {
        for (Map.Entry<String, Map<String, ItemStack>> entry : allAccessories.entrySet()) {
            String handlerName = entry.getKey();

            Map<String, ItemStack> accessories = entry.getValue();

            IAccessoryHandler handler = handlers.get(handlerName);
            if (handler != null) {
                if (dropPos != null && level != null) {

                    handler.restoreAccessories(player, accessories, dropIfFull, level, dropPos);
                } else {
                    handler.restoreAccessories(player, accessories, dropIfFull);
                }

            } else {
                for (ItemStack stack : accessories.values()) {
                    if (!stack.isEmpty()) {
                        if (dropPos != null && level != null) {
                            Containers.dropItemStack(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack.copy());
                        } else {
                            player.drop(stack.copy(), false);
                        }
                    }
                }
            }
        }
    }

    private static Map<String, ItemStack> parseUnknownAccessories(CompoundTag handlerData, RegistryAccess registryAccess) {
        Map<String, ItemStack> parsed = new HashMap<>();

        int syntheticIndex = 0;

        for (String key : handlerData.getAllKeys()) {
            if (!handlerData.contains(key, Tag.TAG_LIST)) {
                continue;
            }

            ListTag items = handlerData.getList(key, Tag.TAG_COMPOUND);

            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                if (!itemTag.contains("Item", Tag.TAG_COMPOUND)) {
                    continue;
                }

                ItemStack stack = ItemStack.parseOptional(registryAccess, itemTag.getCompound("Item"));

                if (stack.isEmpty()) {
                    continue;
                }

                String slotKey = itemTag.getString("SlotKey");

                if (slotKey.isEmpty()) {
                    slotKey = key + "/unknown/" + (syntheticIndex++);
                }

                parsed.put(slotKey, stack);
            }
        }
        return parsed;
    }
}
