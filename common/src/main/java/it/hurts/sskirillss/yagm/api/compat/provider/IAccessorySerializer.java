package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.Map;


public interface IAccessorySerializer {


    CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess);


    Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess);
}
