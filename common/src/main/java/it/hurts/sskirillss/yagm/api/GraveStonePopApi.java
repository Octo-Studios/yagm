package it.hurts.sskirillss.yagm.api;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Experimental
public interface GraveStonePopApi {

    public NonNullList<ItemStack> getOrThrowInventory(NonNullList<ItemStack> cached, Supplier<NonNullList<ItemStack>> supplier);


}

