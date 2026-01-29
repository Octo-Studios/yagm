package it.hurts.sskirillss.yagm.api.item_valuator.providers;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * Item value modifier.
 * Applied after the base value is obtained.
 */
@FunctionalInterface
public interface IValueModifier {

    /**
     * Modifies the value of an item
     *
     * @param stack Item
     * @param currentValue Current value
     * @return Modified value
     */
    double modify(ItemStack stack, double currentValue);

    /**
     * Priority modifier
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Modifier ID (for debugging)
     */
    default String getId() {
        return getClass().getSimpleName();
    }
}
