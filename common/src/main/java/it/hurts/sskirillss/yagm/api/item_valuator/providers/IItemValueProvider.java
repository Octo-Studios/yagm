package it.hurts.sskirillss.yagm.api.item_valuator.providers;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.OptionalDouble;

/**
 * Item value provider.
 * Addons can implement custom evaluation logic.
 */
public interface IItemValueProvider {

    /**
     * Unique provider identifier
     */
    String getId();

    /**
     * Priority provider
     */
    int getPriority();


    OptionalDouble getValue(ItemStack stack);


    default boolean canEvaluate(ItemStack stack) {
        return getValue(stack).isPresent();
    }
}
