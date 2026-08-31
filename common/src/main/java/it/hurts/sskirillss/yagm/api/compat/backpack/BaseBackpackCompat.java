package it.hurts.sskirillss.yagm.api.compat.backpack;

import it.hurts.sskirillss.yagm.api.compat.backpack.handlers.IBackpackHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;


public abstract class BaseBackpackCompat implements IBackpackHandler {

    @Override
    public CompoundTag saveToNBT(List<BackpackEntry> backpacks, RegistryAccess registryAccess) {
        CompoundTag root = new CompoundTag();
        ListTag entries = new ListTag();

        for (BackpackEntry backpack : backpacks) {
            if (backpack.stack().isEmpty()) {
                continue;
            }

            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("SlotKey", backpack.slotKey());
            entryTag.put("Item", BackpackLoader.copyInventoryStack(backpack.stack()).save(registryAccess));

            if (!backpack.metadata().isEmpty()) {
                entryTag.put("Metadata", backpack.metadata().copy());
            }

            entries.add(entryTag);
        }

        root.put("Entries", entries);
        return root;
    }

    @Override
    public List<BackpackEntry> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        List<BackpackEntry> backpacks = new ArrayList<>();

        if (!tag.contains("Entries", Tag.TAG_LIST)) {
            return backpacks;
        }

        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);

        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entryTag = entries.getCompound(i);
            ItemStack stack = ItemStack.parseOptional(registryAccess, entryTag.getCompound("Item"));

            if (stack.isEmpty()) {
                continue;
            }

            CompoundTag metadata = entryTag.contains("Metadata", Tag.TAG_COMPOUND) ? entryTag.getCompound("Metadata").copy() : new CompoundTag();
            backpacks.add(new BackpackEntry(entryTag.getString("SlotKey"), stack, metadata));
        }

        return backpacks;
    }

    @Override
    public BackpackRestoreResult restoreBackpack(ServerPlayer player, BackpackEntry backpack, BackpackRestoreContext context) {
        if (backpack.stack().isEmpty()) {
            return BackpackRestoreResult.success();
        }

        if (restoreLeft(player, backpack)) {
            return BackpackRestoreResult.success();
        }

        if (restoreRight(player, backpack)) {
            return BackpackRestoreResult.success();
        }

        return BackpackRestoreResult.pass(backpack.stack(), "no compatible backpack slot available");
    }

    protected abstract boolean restoreLeft(ServerPlayer player, BackpackEntry backpack);

    protected boolean restoreRight(ServerPlayer player, BackpackEntry backpack) {
        return false;
    }

    protected boolean hasMatchingInventoryStack(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        for (ItemStack item : player.getInventory().items) {
            if (ItemStack.matches(item, stack)) {
                return true;
            }
        }

        for (ItemStack item : player.getInventory().armor) {
            if (ItemStack.matches(item, stack)) {
                return true;
            }
        }

        for (ItemStack item : player.getInventory().offhand) {
            if (ItemStack.matches(item, stack)) {
                return true;
            }
        }
        return false;
    }
}
