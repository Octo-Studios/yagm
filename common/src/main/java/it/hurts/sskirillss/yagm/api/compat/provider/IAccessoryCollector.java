package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public interface IAccessoryCollector {

    default Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        var accessories = new HashMap<>(collectAccessoryReferences(player));
        accessories.replaceAll((slot, stack) -> stack.copy());
        return accessories;
    }

    default Map<String, ItemStack> collectAccessoryReferences(ServerPlayer player) {
        return Map.of();
    }


    void clearAccessories(ServerPlayer player);
}
