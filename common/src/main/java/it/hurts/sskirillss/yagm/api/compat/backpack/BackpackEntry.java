package it.hurts.sskirillss.yagm.api.compat.backpack;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record BackpackEntry(String slotKey, ItemStack stack, CompoundTag metadata) {

    public BackpackEntry {
        slotKey = slotKey == null ? "" : slotKey;
        stack = Objects.requireNonNull(stack, "stack").copy();
        metadata = metadata == null ? new CompoundTag() : metadata.copy();
    }

    public BackpackEntry(String slotKey, ItemStack stack) {
        this(slotKey, stack, new CompoundTag());
    }

    public BackpackEntry copy() {
        return new BackpackEntry(slotKey, stack, metadata);
    }
}
