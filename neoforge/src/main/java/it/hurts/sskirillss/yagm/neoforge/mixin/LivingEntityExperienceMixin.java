package it.hurts.sskirillss.yagm.neoforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityExperienceMixin {
    @WrapOperation(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/world/entity/Entity;)V"))
    private void deferPlayerExperience(LivingEntity entity, Entity entity2, Operation<Void> original, @Share("yagm$experienceDrop") LocalRef<Runnable> experienceDrop) {
        if (entity instanceof ServerPlayer) {
            experienceDrop.set(() -> original.call(entity, entity2));
        } else {
            original.call(entity, entity2);
        }
    }

    @Inject(method = "dropAllDeathLoot", at = @At("RETURN"))
    private void finishPlayerExperience(ServerLevel level, DamageSource source, CallbackInfo ci, @Share("yagm$experienceDrop") LocalRef<Runnable> experienceDrop) {
        Runnable drop = experienceDrop.get();
        if (drop != null) {
            drop.run();
        }
    }
}
