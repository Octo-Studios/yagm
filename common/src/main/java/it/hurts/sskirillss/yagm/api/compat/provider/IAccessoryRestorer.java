package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;


public interface IAccessoryRestorer {


    void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull);
}
