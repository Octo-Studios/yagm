package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.minecraft.core.BlockPos;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.world.Containers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public abstract class BaseAccessoryCompat implements IAccessoryHandler {

    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";
    private static final NbtKeys NBT_KEYS = NbtKeys.INSTANCE;

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

    protected void getInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull) {
        for (ItemStack stack : stacks) {
            getInventoryOrDrop(player, stack, dropIfFull);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull) {
        if (stack.isEmpty()) return;

        if (!player.getInventory().add(stack.copy()) && dropIfFull) {
            player.drop(stack.copy(), false);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull, Level level, BlockPos dropPos) {
        for (ItemStack stack : stacks) {
            getInventoryOrDrop(player, stack, dropIfFull, level, dropPos);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull, Level level, BlockPos dropPos) {
        if (stack.isEmpty()) return;

        if (!player.getInventory().add(stack.copy()) && dropIfFull) {
            Containers.dropItemStack(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack.copy());
        }
    }

    public static List<ItemStack> parseAccessories(RegistryAccess registry, CompoundTag data) {
        if (registry == null || data == null || !AccessoryLoader.hasAnyHandler() || !data.contains(NBT_KEYS.getAccessories(), Tag.TAG_COMPOUND)) {
            return List.of();
        }

        Map<String, Map<String, ItemStack>> accessories;

        try {
            accessories = AccessoryLoader.loadNBT(data.getCompound(NBT_KEYS.getAccessories()), registry);
        } catch (RuntimeException e) {
            YAGMCommon.LOGGER.warn("Failed to load accessories from NBT", e);
            return List.of();
        }

        if (accessories.isEmpty()) {
            return List.of();
        }

        return accessories.values().stream()
                .filter(Objects::nonNull)
                .flatMap(slots -> slots.values().stream())
                .filter(Objects::nonNull)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }
}
