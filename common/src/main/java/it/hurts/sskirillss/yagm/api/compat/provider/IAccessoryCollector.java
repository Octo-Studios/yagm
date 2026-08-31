package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IAccessoryCollector {

    /**
     * @return map: slot key → item ("ring/0", "necklace/cosmetic/1", "chest/necklace/0")
     */
    Map<String, ItemStack> collectAccessories(ServerPlayer player);


    void clearAccessories(ServerPlayer player);
}
