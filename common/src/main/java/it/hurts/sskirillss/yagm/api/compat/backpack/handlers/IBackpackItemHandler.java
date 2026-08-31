package it.hurts.sskirillss.yagm.api.compat.backpack.handlers;

import net.minecraft.world.item.ItemStack;

public interface IBackpackItemHandler {

    String getModName();

    boolean isModLoaded();

    boolean supports(ItemStack stack);

    ItemStack copyForGrave(ItemStack stack);
}
