package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Responsible for equipping items into accessory slots.
 */
public interface IAccessoryEquipper {

    /**
     * Checks whether the given item can be equipped as an accessory by this player.
     *
     * @param player the target player
     * @param stack  the item to check
     * @return true if the item is valid for at least one accessory slot
     */
    boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack);

    /**
     * Attempts to equip an item into the first available accessory slot.
     *
     * @param player the target player
     * @param stack  the item to equip
     * @return true if the item was successfully equipped
     */
    boolean tryEquipAccessory(ServerPlayer player, ItemStack stack);
}
