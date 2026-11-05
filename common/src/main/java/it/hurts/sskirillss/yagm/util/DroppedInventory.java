package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackEntry;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class DroppedInventory {
    private final Map<ItemStack, Consumer<ItemStack>> slots = new IdentityHashMap<>();
    private final NonNullList<ItemStack>[] inventory = InventoryUtils.createLists();
    private final Map<String, Map<String, ItemStack>> accessories = new HashMap<>();
    private final Map<String, List<BackpackEntry>> backpacks = new HashMap<>();

    public DroppedInventory(ServerPlayer player) {
        var inventory = player.getInventory();
        var inventories = List.of(inventory.items, inventory.armor, inventory.offhand);

        for (var i = 0; i < inventories.size(); i++) {
            for (var j = 0; j < inventories.get(i).size(); j++) {
                var section = i;
                var slot = j;
                var stack = inventories.get(i).get(j);
                if (!stack.isEmpty()) {
                    slots.put(stack, dropped -> this.inventory[section].set(slot, dropped.copy()));
                }
            }
        }

        AccessoryLoader.collectAccessoryReferences(player).forEach((handler, accessories) ->
                accessories.forEach((slot, stack) -> slots.put(stack, dropped ->
                        this.accessories.computeIfAbsent(handler, key -> new HashMap<>()).put(slot, dropped.copy()))));

        BackpackLoader.collectBackpackReferences(player).forEach((handler, backpacks) ->
                backpacks.forEach((stack, entry) -> slots.put(stack, dropped ->
                        this.backpacks.computeIfAbsent(handler, key -> new ArrayList<>())
                                .add(new BackpackEntry(entry.slotKey(), dropped, entry.metadata())))));
    }

    public CompoundTag save(ServerPlayer player, Collection<ItemEntity> drops, int experience) {
        var items = new ArrayList<ItemStack>();

        for (var drop : drops) {
            var stack = drop.getItem();
            var slot = slots.remove(stack);
            if (slot == null) {
                items.add(stack);
            } else {
                slot.accept(stack);
            }
        }

        var data = InventoryUtils.saveInventory(player, inventory, accessories, backpacks, experience);
        ItemUtils.saveItemList(player.registryAccess(), data, NbtKeys.INSTANCE.getDroppedItems(), items);
        return data;
    }
}
