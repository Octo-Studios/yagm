package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Responsible for NBT serialization and deserialization of accessories.
 */
public interface IAccessorySerializer {

    /**
     * Saves accessories to an NBT tag.
     *
     * @param accessories    map of slot key → item
     * @param registryAccess registry access for item serialization
     * @return CompoundTag containing serialized accessory data
     */
    CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess);

    /**
     * Loads accessories from an NBT tag.
     *
     * @param tag            NBT tag previously saved by {@link #saveToNBT}
     * @param registryAccess registry access for item deserialization
     * @return map: slot key → item
     */
    Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess);
}
