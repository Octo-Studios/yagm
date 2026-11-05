package it.hurts.sskirillss.yagm.fabric.mixin.compat.requiem;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.hurts.sskirillss.yagm.api.compat.requiem.ReliableRequiemCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class RequiemEntityDropsMixin {
    @WrapOperation(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean captureDeathDrop(Level level, Entity drop, Operation<Boolean> original) {
        return ReliableRequiemCompat.captureDrop((Entity) (Object) this, (ItemEntity) drop) || original.call(level, drop);
    }
}
