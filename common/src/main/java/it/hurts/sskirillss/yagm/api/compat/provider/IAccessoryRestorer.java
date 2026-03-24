package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Responsible for restoring previously collected accessories back to a player.
 */
public interface IAccessoryRestorer {

    /**
     * Restores items to accessory slots from a previously collected map.
     * If the original slot is occupied, falls back to inventory or drops the item.
     *
     * @param player      the target player
     * @param accessories map: slot key → item (produced by {@link IAccessoryCollector#collectAccessories})
     * @param dropIfFull  if true, drop items that do not fit in inventory
     */
    void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull);
}
