package it.hurts.sskirillss.yagm.api.valuator.provider;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;


@FunctionalInterface
public interface IValueModifier {


    double modify(ItemStack stack, double currentValue);


    default int getPriority() {
        return 0;
    }


    default String getId() {
        return getClass().getSimpleName();
    }
}
