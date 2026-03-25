package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

@ApiStatus.Internal
public interface IAccessoryHandler extends IAccessoryCollector, IAccessorySerializer, IAccessoryEquipper, IAccessoryRestorer {


    boolean isModLoaded();


    String getModName();


    default NonNullList<ItemStack> toList(Map<String, ItemStack> accessories) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack stack : accessories.values()) {
            if (!stack.isEmpty()) {
                list.add(stack.copy());
            }
        }
        return list;
    }
}
