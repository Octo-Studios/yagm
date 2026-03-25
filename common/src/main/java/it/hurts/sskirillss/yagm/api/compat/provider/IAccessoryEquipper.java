package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public interface IAccessoryEquipper {


    boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack);


    boolean tryEquipAccessory(ServerPlayer player, ItemStack stack);
}
