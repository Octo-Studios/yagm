package it.hurts.sskirillss.yagm.api.compat.accessories;

import net.minecraft.world.item.ItemStack;

public interface AccessoryContainer {

    String slotName();

    int size();

    ItemStack getMain(int index);

    void setMain(int index, ItemStack stack);

    boolean hasCosmetic();

    ItemStack getCosmetic(int index);

    void setCosmetic(int index, ItemStack stack);

    void markChanged();
}
