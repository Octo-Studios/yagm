package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public class ItemUtils {

    public static void saveInventory(HolderLookup.Provider provider, CompoundTag compound, String key, NonNullList<ItemStack> inventory) {
        ListTag listTag = new ListTag();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = (CompoundTag) BackpackLoader.copyInventoryStack(stack).save(provider);
                itemTag.putByte("Slot", (byte) i);
                listTag.add(itemTag);
            }
        }

        if (!listTag.isEmpty()) {
            compound.put(key, listTag);
        }
    }

    public static void readInventory(HolderLookup.Provider provider, CompoundTag compound, String key, NonNullList<ItemStack> inventory) {
        if (!compound.contains(key, Tag.TAG_LIST)) {
            return;
        }
        
        ListTag listTag = compound.getList(key, Tag.TAG_COMPOUND);
        
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            
            if (slot < inventory.size()) {
                ItemStack stack = ItemStack.parseOptional(provider, itemTag);
                inventory.set(slot, stack);
            }
        }
    }

    public static void saveItemList(HolderLookup.Provider provider, CompoundTag compound, String key, Collection<ItemStack> items) {
        ListTag listTag = new ListTag();

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                listTag.add(BackpackLoader.copyInventoryStack(stack).save(provider));
            }
        }

        if (!listTag.isEmpty()) {
            compound.put(key, listTag);
        }
    }

    public static NonNullList<ItemStack> readItemList(HolderLookup.Provider provider, CompoundTag compound, String key) {
        NonNullList<ItemStack> items = NonNullList.create();

        if (!compound.contains(key, Tag.TAG_LIST)) {
            return items;
        }

        ListTag listTag = compound.getList(key, Tag.TAG_COMPOUND);

        for (int i = 0; i < listTag.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(provider, listTag.getCompound(i));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }
}

