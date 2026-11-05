package it.hurts.sskirillss.yagm.api.compat.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface IGravePlacementHandler {
    default boolean handleFallingCollision(Entity entity, Vec3 previousVelocity, double gravity) {
        return false;
    }

    default Vec3 getPlacementPosition(Entity entity, Vec3 position) {
        return position;
    }

    default Vec3 getLaunchVelocity(Entity entity, Vec3 velocity) {
        return velocity;
    }

    default Direction getPlacementFacing(Level level, BlockPos pos, Direction facing) {
        return facing;
    }
}
