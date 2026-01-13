package it.hurts.sskirillss.yagm.fabric.compat.trinkets;

import dev.emi.trinkets.api.*;
import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class TrinketsCompat implements IAccessoryHandler {

    private static final String TAG_ACCESSORIES = "TrinketsAccessories";
    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("trinkets");
    }

    @Override
    public String getModName() {
        return "Trinkets";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) {
            return accessories;
        }

        TrinketComponent component = componentOpt.get();

        for (var groupEntry : component.getInventory().entrySet()) {
            String groupName = groupEntry.getKey();
            Map<String, TrinketInventory> slots = groupEntry.getValue();

            for (var slotEntry : slots.entrySet()) {
                String slotName = slotEntry.getKey();
                TrinketInventory inv = slotEntry.getValue();

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty()) {
                        // Key format: "group/slot/index" e.g. "chest/necklace/0"
                        String key = groupName + "/" + slotName + "/" + i;
                        accessories.put(key, stack.copy());
                    }
                }
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) {
            return;
        }

        TrinketComponent component = componentOpt.get();

        for (var groupEntry : component.getInventory().entrySet()) {
            Map<String, TrinketInventory> slots = groupEntry.getValue();

            for (var slotEntry : slots.entrySet()) {
                TrinketInventory inv = slotEntry.getValue();

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        if (accessories.isEmpty()) {
            return;
        }

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) {
            for (ItemStack stack : accessories.values()) {
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack.copy())) {
                        if (dropIfFull) {
                            player.drop(stack.copy(), false);
                        }
                    }
                }
            }
            return;
        }

        TrinketComponent component = componentOpt.get();
        Map<String, Map<String, TrinketInventory>> inventory = component.getInventory();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            String key = entry.getKey();
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) continue;

            SlotInfo slotInfo = parseSlotKey(key);
            if (slotInfo == null) {
                if (!tryEquipAccessory(player, stack)) {
                    if (!player.getInventory().add(stack.copy())) {
                        if (dropIfFull) {
                            player.drop(stack.copy(), false);
                        }
                    }
                }
                continue;
            }

            // Try to restore to original slot
            Map<String, TrinketInventory> group = inventory.get(slotInfo.groupName);
            if (group != null) {
                TrinketInventory inv = group.get(slotInfo.slotName);
                if (inv != null && slotInfo.index >= 0 && slotInfo.index < inv.getContainerSize()) {
                    ItemStack existing = inv.getItem(slotInfo.index);
                    if (existing.isEmpty()) {
                        inv.setItem(slotInfo.index, stack.copy());
                        continue;
                    }
                }
            }

            if (!tryEquipAccessory(player, stack)) {
                if (!player.getInventory().add(stack.copy())) {
                    if (dropIfFull) {
                        player.drop(stack.copy(), false);
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            CompoundTag itemTag = new CompoundTag();
            itemTag.putString(TAG_SLOT_KEY, entry.getKey());
            itemTag.put(TAG_ITEM, entry.getValue().save(registryAccess));
            itemsList.add(itemTag);
        }

        tag.put(TAG_ACCESSORIES, itemsList);
        return tag;
    }

    @Override
    public Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, ItemStack> accessories = new HashMap<>();

        if (!tag.contains(TAG_ACCESSORIES, Tag.TAG_LIST)) {
            return accessories;
        }

        ListTag itemsList = tag.getList(TAG_ACCESSORIES, Tag.TAG_COMPOUND);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            String slotKey = itemTag.getString(TAG_SLOT_KEY);
            ItemStack stack = ItemStack.parseOptional(registryAccess, itemTag.getCompound(TAG_ITEM));

            if (!stack.isEmpty() && !slotKey.isEmpty()) {
                accessories.put(slotKey, stack);
            }
        }

        return accessories;
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        return TrinketsApi.getTrinket(stack.getItem()) != null;
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) return false;

        TrinketComponent component = componentOpt.get();

        for (var groupEntry : component.getInventory().entrySet()) {
            Map<String, TrinketInventory> slots = groupEntry.getValue();

            for (var slotEntry : slots.entrySet()) {
                TrinketInventory inv = slotEntry.getValue();

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).isEmpty()) {
                        Set<ResourceLocation> predicates = inv.getSlotType().getValidatorPredicates();
                        if (predicates.isEmpty() || stack.getItem() instanceof Trinket) {
                            inv.setItem(i, stack.copy());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }


    /**
     * Parsed slot key information
     */
    private static class SlotInfo {
        final String groupName;
        final String slotName;
        final int index;

        SlotInfo(String groupName, String slotName, int index) {
            this.groupName = groupName;
            this.slotName = slotName;
            this.index = index;
        }
    }

    /**
     * Parse slot key string to SlotInfo.
     * Format: "group/slot/index"
     * Example: "chest/necklace/0"
     */
    private SlotInfo parseSlotKey(String key) {
        if (key == null || key.isEmpty()) return null;

        String[] parts = key.split("/");

        if (parts.length == 3) {
            try {
                String groupName = parts[0];
                String slotName = parts[1];
                int index = Integer.parseInt(parts[2]);
                return new SlotInfo(groupName, slotName, index);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
