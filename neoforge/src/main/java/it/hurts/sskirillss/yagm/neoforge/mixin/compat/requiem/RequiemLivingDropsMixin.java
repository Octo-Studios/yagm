package it.hurts.sskirillss.yagm.neoforge.mixin.compat.requiem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.hurts.sskirillss.yagm.api.compat.requiem.ReliableRequiemCompat;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(LivingEntity.class)
public abstract class RequiemLivingDropsMixin {
    @WrapOperation(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDrops(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;Ljava/util/Collection;Z)Z", remap = false))
    private boolean captureDeathDrops(LivingEntity entity, DamageSource source, Collection<ItemEntity> drops, boolean recentlyHit, Operation<Boolean> original) {
        var canceled = original.call(entity, source, drops, recentlyHit);

        if (!canceled) {
            drops.removeIf(drop -> ReliableRequiemCompat.captureDrop(entity, drop));
        }

        return canceled;
    }
}
