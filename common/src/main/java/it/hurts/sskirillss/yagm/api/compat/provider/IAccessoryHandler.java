package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

/**
 * Full contract for an accessory system integration.
 * Combines collection, serialization, equipping, and restoration capabilities.
 *
 * <p>Implement this interface to add support for a new accessory mod.
 * Register the implementation via {@link it.hurts.sskirillss.yagm.api.compat.AccessoryManager#registerHandler}.
 *
 * @see IAccessoryCollector
 * @see IAccessorySerializer
 * @see IAccessoryEquipper
 * @see IAccessoryRestorer
 */
@ApiStatus.Internal
public interface IAccessoryHandler extends IAccessoryCollector, IAccessorySerializer, IAccessoryEquipper, IAccessoryRestorer {

    /**
     * @return true if the target accessory mod is present in this game instance
     */
    boolean isModLoaded();

    /**
     * @return the display name of the mod this handler integrates with (used for logging and NBT keys)
     */
    String getModName();

    /**
     * Converts the accessory map into a flat list, useful for cost calculations and loot tables.
     *
     * @param accessories map produced by {@link IAccessoryCollector#collectAccessories}
     * @return flat list of all non-empty item stacks
     */
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
