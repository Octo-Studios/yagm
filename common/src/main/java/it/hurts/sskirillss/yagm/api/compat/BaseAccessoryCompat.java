package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared base for all accessory system integrations.
 *
 * <p>Provides a common implementation of NBT serialization ({@link #saveToNBT} / {@link #loadFromNBT})
 * and inventory/drop fallback logic ({@link #fallbackToInventoryOrDrop}).
 *
 * <p>Each subclass must supply a unique NBT tag key via {@link #getNbtTag()} to avoid
 * data collisions when multiple accessory mods are active simultaneously.
 *
 * <p>Platform-specific integrations (Trinkets, Curios) extend this class directly.
 * The Accessories-specific base ({@link it.hurts.sskirillss.yagm.api.compat.accessories.BaseAccessoriesCompat})
 * extends this class and adds container-based slot management.
 */
@Slf4j
public abstract class BaseAccessoryCompat implements IAccessoryHandler {

    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

    /**
     * @return the NBT list key used to store this handler's data (must be unique per handler)
     */
    protected abstract String getNbtTag();

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

        tag.put(getNbtTag(), itemsList);
        return tag;
    }

    @Override
    public Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, ItemStack> accessories = new HashMap<>();

        if (!tag.contains(getNbtTag(), Tag.TAG_LIST)) {
            return accessories;
        }

        ListTag itemsList = tag.getList(getNbtTag(), Tag.TAG_COMPOUND);

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

    /**
     * Attempts to add each item to the player's inventory; drops items if the inventory is full
     * and {@code dropIfFull} is true.
     */
    protected void fallbackToInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull) {
        for (ItemStack stack : stacks) {
            fallbackToInventoryOrDrop(player, stack, dropIfFull);
        }
    }

    /**
     * Attempts to add a single item to the player's inventory; drops it if the inventory is full
     * and {@code dropIfFull} is true.
     */
    protected void fallbackToInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull) {
        if (stack.isEmpty()) return;

        if (!player.getInventory().add(stack.copy()) && dropIfFull) {
            player.drop(stack.copy(), false);
        }
    }
}
