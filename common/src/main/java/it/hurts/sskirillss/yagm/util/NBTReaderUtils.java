package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NBTReaderUtils {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private static final String[] INVENTORY_KEYS = {
            KEYS.getMainInventory(), KEYS.getArmorInventory(), KEYS.getOffhandInventory()
    };

    public static NonNullList<ItemStack> getAllItemsFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        NonNullList<ItemStack>[] lists = InventoryUtils.createLists();

        for (int i = 0; i < INVENTORY_KEYS.length; i++) {
            ItemUtils.readInventory(provider, nbt, INVENTORY_KEYS[i], lists[i]);
        }

        NonNullList<ItemStack> result = NonNullList.create();

        for (NonNullList<ItemStack> list : lists) {
            for (ItemStack stack : list) {
                if (!stack.isEmpty()) result.add(stack);
            }
        }

        result.addAll(ItemUtils.readItemList(provider, nbt, KEYS.getDroppedItems()));

        return result;
    }

    public static void restoreFromNBT(ServerPlayer player, CompoundTag data, boolean restoreAccessories, BlockPos dropPos) {
        NonNullList<ItemStack>[] lists = InventoryUtils.createLists();
        var reg = player.registryAccess();

        for (int i = 0; i < INVENTORY_KEYS.length; i++) {
            ItemUtils.readInventory(reg, data, INVENTORY_KEYS[i], lists[i]);
        }

        Inventory inv = player.getInventory();
        ContainerUtils.restoreInventory(inv.items, lists[0], player, dropPos);
        ContainerUtils.restoreInventory(inv.armor, lists[1], player, dropPos);
        ContainerUtils.restoreInventory(inv.offhand, lists[2], player, dropPos);

        for (ItemStack stack : ItemUtils.readItemList(reg, data, KEYS.getDroppedItems())) {
            ContainerUtils.giveOrDropItem(player, stack.copy(), dropPos);
        }

        if (restoreAccessories && AccessoryLoader.hasAnyHandler() && data.contains(KEYS.getAccessories(), 10)) {
            var accessories = AccessoryLoader.loadNBT(data.getCompound(KEYS.getAccessories()), reg);
            AccessoryLoader.restoreAccessories(player, accessories, true, dropPos, player.level());
        }

        if (data.contains(KEYS.getBackpacks(), Tag.TAG_COMPOUND)) {
            var backpacks = BackpackLoader.loadNBT(data.getCompound(KEYS.getBackpacks()), reg);
            BackpackLoader.restoreBackpacks(player, backpacks, true, dropPos, player.level());
        }
    }
}
