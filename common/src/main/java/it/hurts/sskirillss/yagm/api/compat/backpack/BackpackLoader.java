package it.hurts.sskirillss.yagm.api.compat.backpack;

import it.hurts.sskirillss.yagm.api.compat.backpack.handlers.IBackpackHandler;
import it.hurts.sskirillss.yagm.api.compat.backpack.handlers.IBackpackItemHandler;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class BackpackLoader {

    private static final Map<String, IBackpackHandler> handlers = new ConcurrentHashMap<>();
    private static final Map<String, IBackpackItemHandler> itemHandlers = new ConcurrentHashMap<>();

    public static void registerHandler(IBackpackHandler handler) {
        if (handler == null) return;

        String modName = requireModName(handler.getModName(), "backpack handler");
        if (handler.isModLoaded()) {
            handlers.put(modName, handler);
            log.info("[YAGM] Registered backpack handler: {}", modName);
        } else {
            log.debug("[YAGM] Skipped unavailable backpack handler: {}", modName);
        }
    }

    public static boolean hasHandler() {
        return !handlers.isEmpty();
    }

    public static void registerItemHandler(IBackpackItemHandler handler) {
        if (handler == null) return;

        String modName = requireModName(handler.getModName(), "backpack item handler");
        if (handler.isModLoaded()) {
            itemHandlers.put(modName, handler);
            log.info("[YAGM] Registered backpack item handler: {}", modName);
        } else {
            log.debug("[YAGM] Skipped unavailable backpack item handler: {}", modName);
        }
    }

    public static ItemStack copyInventoryStack(ItemStack stack) {
        Objects.requireNonNull(stack, "stack");

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        for (IBackpackItemHandler handler : itemHandlers.values()) {
            if (handler.supports(stack)) {
                ItemStack copy = handler.copyForGrave(stack);
                if (copy == null || copy.isEmpty()) {
                    throw new IllegalStateException("Backpack item handler '" + handler.getModName() + "' returned an empty copy for a non-empty stack");
                }

                return copy;
            }
        }

        return stack.copy();
    }

    public static Map<String, List<BackpackEntry>> collectBackpacks(ServerPlayer player) {
        Map<String, List<BackpackEntry>> allBackpacks = new HashMap<>();

        for (Map.Entry<String, IBackpackHandler> registeredHandler : handlers.entrySet()) {
            String modName = registeredHandler.getKey();

            IBackpackHandler handler = registeredHandler.getValue();
            List<BackpackEntry> backpacks = copyEntries(modName, handler.collectBackpacks(player));

            if (!backpacks.isEmpty()) {
                allBackpacks.put(modName, backpacks);
            }
        }

        return allBackpacks;
    }

    public static void clearBackpacks(ServerPlayer player) {
        for (IBackpackHandler handler : handlers.values()) {
            handler.clearBackpacks(player);
        }
    }

    public static CompoundTag saveNBT(Map<String, List<BackpackEntry>> allBackpacks, RegistryAccess registryAccess) {
        CompoundTag root = new CompoundTag();

        for (Map.Entry<String, List<BackpackEntry>> entry : allBackpacks.entrySet()) {

            IBackpackHandler handler = handlers.get(entry.getKey());

            if (handler == null) {
                throw new IllegalStateException("No backpack handler registered for '" + entry.getKey() + "'");
            }

            List<BackpackEntry> backpacks = copyEntries(entry.getKey(), entry.getValue());
            if (backpacks.isEmpty()) {
                continue;
            }

            CompoundTag handlerData = Objects.requireNonNull(handler.saveToNBT(backpacks, registryAccess), "handlerData");

            if (handlerData.isEmpty()) {
                throw new IllegalStateException("Backpack handler '" + entry.getKey() + "' serialized a non-empty snapshot into empty NBT");
            }

            root.put(entry.getKey(), handlerData);
        }

        return root;
    }

    public static Map<String, List<BackpackEntry>> loadNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, List<BackpackEntry>> allBackpacks = new HashMap<>();

        for (String handlerName : tag.getAllKeys()) {

            IBackpackHandler handler = handlers.get(handlerName);

            CompoundTag handlerData = tag.getCompound(handlerName);
            List<BackpackEntry> backpacks;

            if (handler != null) {
                backpacks = copyEntries(handlerName, handler.loadFromNBT(handlerData, registryAccess));
            } else {
                backpacks = parseUnknownBackpacks(handlerData, registryAccess);

                if (!backpacks.isEmpty()) {
                    log.warn("[YAGM] No backpack handler for '{}', {} backpacks will use inventory/drop fallback", handlerName, backpacks.size());
                }
            }

            if (!backpacks.isEmpty()) {
                allBackpacks.put(handlerName, backpacks);
            }
        }

        return allBackpacks;
    }

    public static List<ItemStack> parseBackpacks(RegistryAccess registryAccess, CompoundTag data, String handlerName) {
        if (registryAccess == null || data == null || handlerName == null || !data.contains(NbtKeys.INSTANCE.getBackpacks(), Tag.TAG_COMPOUND)) {
            return List.of();
        }

        CompoundTag root = data.getCompound(NbtKeys.INSTANCE.getBackpacks());
        if (!root.contains(handlerName, Tag.TAG_COMPOUND)) {
            return List.of();
        }

        return parseUnknownBackpacks(root.getCompound(handlerName), registryAccess).stream()
                .map(BackpackEntry::stack)
                .map(ItemStack::copy)
                .toList();
    }

    public static void restoreBackpacks(ServerPlayer player, Map<String, List<BackpackEntry>> allBackpacks, boolean dropIfFull, BlockPos dropPos, Level level) {
        BackpackRestoreContext context = new BackpackRestoreContext(dropIfFull, level, dropPos);

        for (Map.Entry<String, List<BackpackEntry>> entry : allBackpacks.entrySet()) {

            IBackpackHandler handler = handlers.get(entry.getKey());
            List<BackpackEntry> backpacks = copyEntries(entry.getKey(), entry.getValue());

            for (BackpackEntry backpack : backpacks) {
                if (handler == null) {
                    fallback(player, backpack.stack(), context);
                    continue;
                }

                BackpackRestoreResult result = Objects.requireNonNull(handler.restoreBackpack(player, backpack, context), "restoreResult");

                if (!result.restored() && result.remainder().isEmpty()) {
                    throw new IllegalStateException("Backpack handler '" + entry.getKey() + "' did not restore '" + backpack.slotKey() + "' or return a fallback stack");
                }

                if (!result.remainder().isEmpty()) {
                    for (ItemStack stack : result.remainder()) {
                        fallback(player, stack, context);
                    }
                }
            }
        }
    }

    public static void dropBackpacks(Level level, BlockPos pos, Map<String, List<BackpackEntry>> allBackpacks) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (List<BackpackEntry> backpacks : allBackpacks.values()) {
            for (BackpackEntry backpack : backpacks) {
                if (!backpack.stack().isEmpty()) {
                    Containers.dropItemStack(level, x, y, z, backpack.stack().copy());
                }
            }
        }
    }

    private static List<BackpackEntry> copyEntries(String modName, List<BackpackEntry> entries) {
        if (entries == null) {
            throw new IllegalStateException("Backpack handler '" + modName + "' returned null entries");
        }

        List<BackpackEntry> copied = new ArrayList<>();

        for (BackpackEntry entry : entries) {
            if (entry == null) {
                throw new IllegalStateException("Backpack handler '" + modName + "' returned a null entry");
            }

            if (!entry.stack().isEmpty()) {
                copied.add(entry.copy());
            }
        }

        return List.copyOf(copied);
    }

    private static String requireModName(String modName, String handlerType) {
        if (modName == null || modName.isBlank()) {
            throw new IllegalArgumentException("Cannot register " + handlerType + " without a mod id");
        }

        return modName;
    }

    private static void fallback(ServerPlayer player, ItemStack stack, BackpackRestoreContext context) {
        if (stack.isEmpty()) {
            return;
        }

        if (player.getInventory().add(stack.copy())) {
            return;
        }

        if (!context.dropIfFull()) {
            return;
        }

        if (context.hasDropPosition()) {
            BlockPos dropPos = context.dropPos();
            Containers.dropItemStack(context.level(), dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack.copy());
        } else {
            player.drop(stack.copy(), false);
        }
    }

    private static List<BackpackEntry> parseUnknownBackpacks(CompoundTag handlerData, RegistryAccess registryAccess) {
        List<BackpackEntry> backpacks = new ArrayList<>();

        if (!handlerData.contains("Entries", Tag.TAG_LIST)) {
            return backpacks;
        }

        ListTag entries = handlerData.getList("Entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            if (!entryTag.contains("Item", Tag.TAG_COMPOUND)) {
                continue;
            }

            ItemStack stack = ItemStack.parseOptional(registryAccess, entryTag.getCompound("Item"));

            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag metadata = entryTag.contains("Metadata", Tag.TAG_COMPOUND) ? entryTag.getCompound("Metadata").copy() : new CompoundTag();

            backpacks.add(new BackpackEntry(entryTag.getString("SlotKey"), stack, metadata));
        }

        return backpacks;
    }
}
