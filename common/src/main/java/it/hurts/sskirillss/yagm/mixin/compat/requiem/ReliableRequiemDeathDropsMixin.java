package it.hurts.sskirillss.yagm.mixin.compat.requiem;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.hurts.sskirillss.yagm.api.compat.requiem.ReliableRequiemCompat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class ReliableRequiemDeathDropsMixin {
    @WrapMethod(method = "dropAllDeathLoot")
    private void handleRequiemDeathDrops(ServerLevel level, DamageSource source, Operation<Void> original) {
        if ((Object) this instanceof ServerPlayer player && !player.isSpectator()
                && ReliableRequiemCompat.isEnabled() && !level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            ReliableRequiemCompat.handleDeathDrops(player, () -> original.call(level, source));
        } else {
            original.call(level, source);
        }
    }

    @WrapOperation(method = "dropExperience", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void captureRequiemExperience(ServerLevel level, Vec3 position, int amount, Operation<Void> original) {
        if (!ReliableRequiemCompat.captureExperience((LivingEntity) (Object) this, amount)) {
            original.call(level, position, amount);
        }
    }
}
