package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Map;


public interface IAccessoryRestorer {

    void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull);

    default void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull, Level level, BlockPos dropPos) {
        restoreAccessories(player, accessories, dropIfFull);
    }
}
