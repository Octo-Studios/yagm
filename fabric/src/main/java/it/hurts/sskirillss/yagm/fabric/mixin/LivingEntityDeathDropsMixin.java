package it.hurts.sskirillss.yagm.fabric.mixin;

import it.hurts.sskirillss.yagm.event.GraveStoneEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathDropsMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void handleLatePlayerDeath(ServerLevel level, DamageSource source, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            GraveStoneEvent.handlePlayerDeath(player);
        }
    }
}
