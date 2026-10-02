package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class ArmorUtils {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    public static NonNullList<ItemStack> parseArmor(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getArmorInventory(), armor);
        return armor;
    }

    public static NonNullList<ItemStack> parseMainInventory(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> main = NonNullList.withSize(36, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getMainInventory(), main);
        return main;
    }

    public static NonNullList<ItemStack> parseOffHand(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getOffhandInventory(), offhand);
        return offhand;
    }
}
