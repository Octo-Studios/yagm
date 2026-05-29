package it.hurts.sskirillss.yagm.mixin;

import it.hurts.sskirillss.yagm.init.ItemsRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void yagm$applyRestoreKeyBowLikeFov(CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack using = player.getUseItem();
        if (!using.is(ItemsRegistry.RESTORE_KEY.get())) {
            return;
        }

        float base = cir.getReturnValueF();
        float useTicks = using.getUseDuration(player) - player.getUseItemRemainingTicks();
        float charge = Mth.clamp(useTicks / 20.0F, 0.0F, 1.0F);
        charge *= charge;

        cir.setReturnValue(base * (1.0F - charge * 0.15F));
    }
}
