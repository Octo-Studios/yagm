package it.hurts.sskirillss.yagm.neoforge.mixin;

import it.hurts.sskirillss.yagm.neoforge.compat.twilight.TwilightCharmSlotHook;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "twilightforest.events.CharmEvents")
public abstract class TwilightCharmEventsMixin {
    @Redirect(method = "applyCharm", at = @At(value = "INVOKE", target = "Ltwilightforest/util/TFItemStackUtils;consumeInventoryItem(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ItemLike;Lnet/minecraft/nbt/CompoundTag;Z)Z"))
    private static boolean yagm$preferEquippedCharm(Player player, ItemLike itemLike, CompoundTag data, boolean saveCharm) {
        return TwilightCharmSlotHook.consumeInventoryItemPreferringEquipped(player, itemLike, data, saveCharm);
    }

    @Redirect(method = "applyCharm", at = @At(value = "INVOKE", target = "Ltwilightforest/events/CharmEvents;hasCharmCurio(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/entity/player/Player;)Z"))
    private static boolean yagm$consumeEquippedCharm(Item item, Player player) {
        return TwilightCharmSlotHook.consumeEquippedCharm(item, player);
    }
}
