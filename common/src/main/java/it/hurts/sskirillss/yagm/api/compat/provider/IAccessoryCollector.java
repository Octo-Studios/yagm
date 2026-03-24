package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Responsible for collecting and clearing accessories from a player's slots.
 */
public interface IAccessoryCollector {

    /**
     * Collects all items from the player's accessory slots.
     * Called when the player dies BEFORE clearing the inventory.
     *
     * @return map: slot key → item (e.g. "ring/0", "necklace/cosmetic/1", "chest/necklace/0")
     */
    Map<String, ItemStack> collectAccessories(ServerPlayer player);

    /**
     * Clears all player accessory slots.
     * Called after collecting items.
     */
    void clearAccessories(ServerPlayer player);
}
