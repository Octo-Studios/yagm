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


@Slf4j
public abstract class BaseAccessoryCompat implements IAccessoryHandler {

    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

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

    protected void fallbackToInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull) {
        for (ItemStack stack : stacks) {
            fallbackToInventoryOrDrop(player, stack, dropIfFull);
        }
    }


    protected void fallbackToInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull) {
        if (stack.isEmpty()) return;

        if (!player.getInventory().add(stack.copy()) && dropIfFull) {
            player.drop(stack.copy(), false);
        }
    }
}
