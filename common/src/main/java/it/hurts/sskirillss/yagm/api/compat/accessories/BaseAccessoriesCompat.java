package it.hurts.sskirillss.yagm.api.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class BaseAccessoriesCompat implements IAccessoryHandler {

    private static final String TAG_ACCESSORIES = "AccessoriesAccessories";
    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

    @Override
    public String getModName() {
        return "Accessories";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();
        Optional<Map<String, AccessoryContainerView>> containersOpt = getContainers(player);

        if (containersOpt.isEmpty()) {
            return accessories;
        }

        for (AccessoryContainerView container : containersOpt.get().values()) {
            String slotName = container.slotName();
            int size = container.size();

            for (int i = 0; i < size; i++) {
                ItemStack stack = container.getMain(i);
                if (!stack.isEmpty()) {
                    String key = slotName + "/" + i;
                    accessories.put(key, stack.copy());
                }
            }

            if (container.hasCosmetic()) {
                for (int i = 0; i < size; i++) {
                    ItemStack stack = container.getCosmetic(i);
                    if (!stack.isEmpty()) {
                        String key = slotName + "/cosmetic/" + i;
                        accessories.put(key, stack.copy());
                    }
                }
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<Map<String, AccessoryContainerView>> containersOpt = getContainers(player);

        if (containersOpt.isEmpty()) {
            return;
        }

        for (AccessoryContainerView container : containersOpt.get().values()) {
            int size = container.size();

            for (int i = 0; i < size; i++) {
                container.setMain(i, ItemStack.EMPTY);
            }

            if (container.hasCosmetic()) {
                for (int i = 0; i < size; i++) {
                    container.setCosmetic(i, ItemStack.EMPTY);
                }
            }

            container.markChanged();
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        if (accessories.isEmpty()) {
            return;
        }

        Optional<Map<String, AccessoryContainerView>> containersOpt = getContainers(player);
        if (containersOpt.isEmpty()) {
            fallbackToInventoryOrDrop(player, accessories.values(), dropIfFull);
            return;
        }

        Map<String, AccessoryContainerView> containers = containersOpt.get();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            String key = entry.getKey();
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) {
                continue;
            }

            SlotInfo slotInfo = parseSlotKey(key);
            boolean restored = false;

            if (slotInfo != null) {
                AccessoryContainerView container = containers.get(slotInfo.slotName);
                if (container != null && slotInfo.index >= 0 && slotInfo.index < container.size()) {
                    ItemStack existing = slotInfo.cosmetic ? container.getCosmetic(slotInfo.index) : container.getMain(slotInfo.index);
                    boolean hasTarget = !slotInfo.cosmetic || container.hasCosmetic();

                    if (hasTarget && existing.isEmpty()) {
                        if (slotInfo.cosmetic) {
                            container.setCosmetic(slotInfo.index, stack.copy());
                        } else {
                            container.setMain(slotInfo.index, stack.copy());
                        }
                        container.markChanged();
                        restored = true;
                    }
                }
            }

            if (!restored) {
                if (!tryEquipAccessory(player, stack)) {
                    fallbackToInventoryOrDrop(player, stack, dropIfFull);
                }
            }
        }
    }

    @Override
    public CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

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
        if (stack.isEmpty()) {
            return false;
        }

        return canEquipAccessory(player, stack);
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return tryEquipAccessoryInternal(player, stack);
    }

    protected abstract Optional<Map<String, AccessoryContainerView>> getContainers(ServerPlayer player);

    protected abstract boolean canEquipAccessory(ServerPlayer player, ItemStack stack);

    protected abstract boolean tryEquipAccessoryInternal(ServerPlayer player, ItemStack stack);

    private void fallbackToInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull) {
        for (ItemStack stack : stacks) {
            fallbackToInventoryOrDrop(player, stack, dropIfFull);
        }
    }

    private void fallbackToInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull) {
        if (stack.isEmpty()) {
            return;
        }

        if (!player.getInventory().add(stack.copy()) && dropIfFull) {
            player.drop(stack.copy(), false);
        }
    }

    private static class SlotInfo {
        final String slotName;
        final int index;
        final boolean cosmetic;

        SlotInfo(String slotName, int index, boolean cosmetic) {
            this.slotName = slotName;
            this.index = index;
            this.cosmetic = cosmetic;
        }
    }

    private SlotInfo parseSlotKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String[] parts = key.split("/");
        if (parts.length == 2) {
            try {
                String slotName = parts[0];
                int index = Integer.parseInt(parts[1]);
                return new SlotInfo(slotName, index, false);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (parts.length == 3 && "cosmetic".equals(parts[1])) {
            try {
                String slotName = parts[0];
                int index = Integer.parseInt(parts[2]);
                return new SlotInfo(slotName, index, true);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }
}
