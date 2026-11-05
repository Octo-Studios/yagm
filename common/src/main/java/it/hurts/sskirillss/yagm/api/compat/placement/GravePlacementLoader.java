package it.hurts.sskirillss.yagm.api.compat.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class GravePlacementLoader {
    private static final List<IGravePlacementHandler> handlers = new ArrayList<>();

    public static void registerHandler(IGravePlacementHandler handler) {
        handlers.add(handler);
    }

    public static boolean handleFallingCollision(Entity entity, Vec3 previousVelocity, double gravity) {
        for (var handler : handlers)
            if (handler.handleFallingCollision(entity, previousVelocity, gravity))
                return true;

        return false;
    }

    public static Vec3 getPlacementPosition(Entity entity) {
        var position = entity.position();

        for (var handler : handlers)
            position = handler.getPlacementPosition(entity, position);

        return position;
    }

    public static Vec3 getLaunchVelocity(Entity entity, Vec3 velocity) {
        for (var handler : handlers)
            velocity = handler.getLaunchVelocity(entity, velocity);

        return velocity;
    }

    public static Direction getPlacementFacing(Level level, BlockPos pos, Direction facing) {
        for (var handler : handlers)
            facing = handler.getPlacementFacing(level, pos, facing);

        return facing;
    }
}
