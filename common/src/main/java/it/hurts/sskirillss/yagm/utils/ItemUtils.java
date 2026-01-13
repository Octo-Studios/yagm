package it.hurts.sskirillss.yagm.utils;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public class ItemUtils {


    public static void saveInventory(HolderLookup.Provider provider, CompoundTag compound, String key, NonNullList<ItemStack> inventory) {
        ListTag listTag = new ListTag();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = (CompoundTag) stack.save(provider);
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
}

