package it.hurts.sskirillss.yagm.fabric.compat.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessoriesCompat implements IAccessoryHandler {

    private static final String TAG_ACCESSORIES = "AccessoriesAccessories";
    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("accessories");
    }

    @Override
    public String getModName() {
        return "Accessories";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
            return accessories;
        }

        AccessoriesCapability capability = capabilityOpt.get();

        for (AccessoriesContainer container : capability.getContainers().values()) {
            String slotName = container.getSlotName();
            int size = container.getSize();

            ExpandedSimpleContainer main = container.getAccessories();
            for (int i = 0; i < size; i++) {
                ItemStack stack = main.getItem(i);
                if (!stack.isEmpty()) {
                    String key = slotName + "/" + i;
                    accessories.put(key, stack.copy());
                }
            }

            ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
            if (cosmetic != null) {
                for (int i = 0; i < size; i++) {
                    ItemStack stack = cosmetic.getItem(i);
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
        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
            return;
        }

        AccessoriesCapability capability = capabilityOpt.get();

        for (AccessoriesContainer container : capability.getContainers().values()) {
            int size = container.getSize();
            ExpandedSimpleContainer main = container.getAccessories();
            for (int i = 0; i < size; i++) {
                main.setItem(i, ItemStack.EMPTY);
            }

            ExpandedSimpleContainer cosmetic = container.getCosmeticAccessories();
            if (cosmetic != null) {
                for (int i = 0; i < size; i++) {
                    cosmetic.setItem(i, ItemStack.EMPTY);
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

        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
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

        AccessoriesCapability capability = capabilityOpt.get();
        Map<String, AccessoriesContainer> containers = capability.getContainers();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            String key = entry.getKey();
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) {
                continue;
            }

            SlotInfo slotInfo = parseSlotKey(key);
            boolean restored = false;

            if (slotInfo != null) {
                AccessoriesContainer container = containers.get(slotInfo.slotName);
                if (container != null) {
                    ExpandedSimpleContainer target = slotInfo.cosmetic ? container.getCosmeticAccessories() : container.getAccessories();
                    if (target != null && slotInfo.index >= 0 && slotInfo.index < container.getSize()) {
                        ItemStack existing = target.getItem(slotInfo.index);
                        if (existing.isEmpty()) {
                            target.setItem(slotInfo.index, stack.copy());
                            container.markChanged();
                            restored = true;
                        }
                    }
                }
            }

            if (!restored) {
                if (!tryEquipAccessory(player, stack)) {
                    if (!player.getInventory().add(stack.copy())) {
                        if (dropIfFull) {
                            player.drop(stack.copy(), false);
                        }
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

        return !AccessoriesAPI.getStackSlotTypes(player, stack).isEmpty();
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
            return false;
        }

        AccessoriesCapability capability = capabilityOpt.get();
        var slotReference = capability.attemptToEquipAccessory(stack.copy());
        return slotReference != null && slotReference.isValid();
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
