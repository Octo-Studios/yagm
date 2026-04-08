package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class GhostWanderGoal extends Goal {

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
        return ghost.isTame() && ghost.getBehaviorMode() == BehaviorMode.WANDER;
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

    @Override
    public void tick() {
        wobbleTick++;
        wanderTick++;

        if (wanderTarget == null || wanderTick >= GhostEntityData.WANDER_RETARGET_TICKS) {
            wanderTick = 0;
            wanderTarget = ghost.blockPosition().offset(ghost.getRandom().nextInt(16) - 8, 0, ghost.getRandom().nextInt(16) - 8);
        }

        Vec3 toTarget = Vec3.atCenterOf(wanderTarget).add(0, 1.5, 0).subtract(ghost.position());
        if (toTarget.horizontalDistance() < GhostEntityData.WANDER_ARRIVAL_DIST) {
            ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.8));
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

        ghost.setDeltaMovement(velocity);

        if (velocity.horizontalDistanceSqr() > 1e-4) {
            float targetYaw = (float) Math.toDegrees(Mth.atan2(-velocity.x, velocity.z));
            ghost.setYRot(Mth.rotLerp(GhostEntityData.YAW_NORMAL, ghost.getYRot(), targetYaw));
            ghost.yHeadRot = ghost.yBodyRot = ghost.getYRot();
        }
    }

}
