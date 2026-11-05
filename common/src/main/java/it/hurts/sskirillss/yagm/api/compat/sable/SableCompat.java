package it.hurts.sskirillss.yagm.api.compat.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import it.hurts.sskirillss.yagm.api.compat.placement.IGravePlacementHandler;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SableCompat implements IGravePlacementHandler {
    public static void onBlocksMoved(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks) {
        if (!ConfigRegistry.isCompatEnabled("sable"))
            return;

        var destination = transform.getLevel();
        var cemeteries = CemeteryManager.getInstance();

        for (var pos : blocks) {
            var newPos = transform.apply(pos);
            if (!(destination.getBlockEntity(newPos) instanceof GraveStoneBlockEntity grave) || grave.isDecorative())
                continue;

            GraveDataManager.get(level).setGravePos(grave.getGraveData().getGraveId(), newPos);
            cemeteries.removeGrave(level.dimension(), pos);
            cemeteries.addGrave(destination.dimension(), newPos);
            CemeterySavedData.markDirty(level);
        }
    }

    @Override
    public boolean handleFallingCollision(Entity entity, Vec3 previousVelocity, double gravity) {
        var subLevel = SableCompanion.INSTANCE.getTrackingSubLevel(entity);
        if (subLevel == null)
            return false;

        var collisions = ((EntityMovementExtension) entity).sable$getCollisionInfo();
        if (collisions == null || collisions.firstCollisions == null)
            return false;

        var collision = collisions.firstCollisions.get(subLevel);
        if (collision == null)
            return false;

        var normal = new Vec3(collision.globalDirection().x(), collision.globalDirection().y(), collision.globalDirection().z());
        var localNormal = subLevel.logicalPose().transformNormalInverse(normal);
        var surface = Direction.getNearest(localNormal.x, localNormal.y, localNormal.z);
        if (surface == Direction.UP && normal.y > 0)
            return false;

        var velocity = previousVelocity.add(0, gravity, 0);
        velocity = velocity.subtract(normal.scale(Math.min(0, velocity.dot(normal))));

        if (velocity.lengthSqr() == 0) {
            var tangent = surface.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
            var direction = subLevel.logicalPose().transformNormal(Vec3.atLowerCornerOf(tangent.getNormal()));
            direction = direction.subtract(normal.scale(direction.dot(normal)));
            velocity = direction.normalize().scale(-gravity);
        }

        entity.setDeltaMovement(velocity);
        entity.setOnGround(false);
        return true;
    }

    @Override
    public Vec3 getPlacementPosition(Entity entity, Vec3 position) {
        var subLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);

        return subLevel == null ? position : subLevel.logicalPose().transformPositionInverse(position);
    }

    @Override
    public Vec3 getLaunchVelocity(Entity entity, Vec3 velocity) {
        var subLevel = SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(entity);

        if (subLevel == null)
            return velocity;

        var localPos = subLevel.logicalPose().transformPositionInverse(entity.position());

        return velocity.add(SableCompanion.INSTANCE.getVelocity(entity.level(), subLevel, localPos).scale(1.0 / 20.0));
    }

    @Override
    public Direction getPlacementFacing(Level level, BlockPos pos, Direction facing) {
        var subLevel = SableCompanion.INSTANCE.getContaining(level, pos);

        if (subLevel == null)
            return facing;

        var direction = subLevel.logicalPose().transformNormalInverse(Vec3.atLowerCornerOf(facing.getNormal()));

        return Direction.getNearest(direction.x, 0, direction.z);
    }
}
