package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class GhostWanderGoal extends Goal {

    private static final int UNTAMED_HOME_RADIUS = 7;
    private static final int UNTAMED_HARD_RETURN_RADIUS = 9;

    private final GhostEntity ghost;
    private BlockPos wanderTarget = null;
    private int wanderTick = 0;
    private int wobbleTick = 0;

    public GhostWanderGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return ghost.getTarget() == null && !ghost.isInLove() && (!ghost.isTame() || ghost.getBehaviorMode() == BehaviorMode.WANDER);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        wanderTarget = null;
        wanderTick = 0;
        wobbleTick = 0;
    }

    private void pickNewTarget() {
        wanderTick = 0;

        if (!ghost.isTame() && ghost.getHomePos() == null) {
            ghost.setHomePos(ghost.blockPosition());
        }

        BlockPos anchor = ghost.blockPosition();
        if (!ghost.isTame() && ghost.getHomePos() != null) {
            anchor = ghost.getHomePos();

            double homeDistSqr = ghost.distanceToSqr(anchor.getX() + 0.5, ghost.getY(), anchor.getZ() + 0.5);
            if (homeDistSqr > UNTAMED_HARD_RETURN_RADIUS * UNTAMED_HARD_RETURN_RADIUS) {
                int surfaceY = ghost.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, anchor.getX(), anchor.getZ());
                int targetY = surfaceY + 2 + ghost.getRandom().nextInt(3);
                wanderTarget = new BlockPos(anchor.getX(), targetY, anchor.getZ());
                return;
            }
        }

        int radius = !ghost.isTame() && ghost.getHomePos() != null ? UNTAMED_HOME_RADIUS : 8;
        int dx = ghost.getRandom().nextInt(radius * 2 + 1) - radius;
        int dz = ghost.getRandom().nextInt(radius * 2 + 1) - radius;
        int baseX = anchor.getX() + dx;
        int baseZ = anchor.getZ() + dz;
        int surfaceY = ghost.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, baseX, baseZ);
        int targetY = surfaceY + 1 + ghost.getRandom().nextInt(5); // 1–5 blocks above surface
        wanderTarget = new BlockPos(baseX, targetY, baseZ);
    }

    @Override
    public void tick() {
        wobbleTick++;
        wanderTick++;

        if (wanderTarget == null || wanderTick >= GhostEntityData.WANDER_RETARGET_TICKS) {
            pickNewTarget();
        }

        Vec3 toTarget = Vec3.atCenterOf(wanderTarget).subtract(ghost.position());
        if (toTarget.horizontalDistance() < GhostEntityData.WANDER_ARRIVAL_DIST) {
            ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.8));
            pickNewTarget();
            return;
        }

        Vec3 direction = toTarget.normalize();
        Vec3 perpendicular = new Vec3(-direction.z, 0, direction.x);
        double wobble = Math.sin(wobbleTick * GhostEntityData.WOBBLE_SPEED * 0.6) * GhostEntityData.WOBBLE_AMPLITUDE * 1.5;
        double bob = Math.sin(wobbleTick * GhostEntityData.BOB_SPEED) * GhostEntityData.BOB_AMPLITUDE;

        Vec3 desired = direction.scale(GhostEntityData.FLY_SPEED * GhostEntityData.WANDER_SPEED_MULT).add(perpendicular.scale(wobble)).add(0, bob, 0);

        Vec3 velocity = ghost.getDeltaMovement().add(desired.scale(0.4));
        double speed = velocity.length();
        double max = GhostEntityData.FLY_SPEED * GhostEntityData.WANDER_SPEED_MULT;

        if (speed > max) {
            velocity = velocity.scale(max / speed);
        }

        ghost.setSteeredDeltaMovement(velocity, GhostEntityData.YAW_WANDER_TURN_DEGREES, GhostEntityData.YAW_TURN_MIN_SPEED_FACTOR);
    }

}
