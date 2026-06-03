package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.GhostMood;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class GhostSnippedAttackGoal extends Goal {

    private final GhostEntity ghost;
    private static final double RETREAT_DISTANCE = 5.0;
    private static final double CHARGE_SPEED = 0.72;
    private int attackCooldown = 0;
    private Vec3 retreatTarget;

    public GhostSnippedAttackGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = ghost.getTarget();
        if (ghost.hasTameAttackDelay()) {
            return false;
        }

        return target != null && target.isAlive() && !(ghost.isTame() && target instanceof Player player && ghost.isOwnedBy(player));
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ghost.setMood(GhostMood.ANGRY);
    }

    @Override
    public void stop() {
        ghost.getNavigation().stop();
        retreatTarget = null;

        if (!ghost.isTame() && ghost.getTarget() == null) {
            ghost.setMood(GhostMood.DEFAULT);
        }
    }

    @Override
    public void tick() {
        LivingEntity target = ghost.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5, 0);
        Vec3 toTarget = targetPos.subtract(ghost.position());

        if (attackCooldown > 0) {
            attackCooldown--;
            ghost.getNavigation().stop();
            faceVector(toTarget, 0.42f);

            if (retreatTarget != null) {
                Vec3 toRetreat = retreatTarget.subtract(ghost.position());
                double retreatDistance = toRetreat.length();

                if (retreatDistance > 0.08) {
                    Vec3 retreatStep = toRetreat.scale(Math.min(0.32, retreatDistance) / Math.max(retreatDistance, 1e-6));
                    Vec3 retreatVelocity = ghost.getDeltaMovement().scale(0.55).add(retreatStep.scale(0.45));
                    ghost.setDeltaMovement(retreatVelocity);
                } else {
                    retreatTarget = null;
                    ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.6));
                }
            }
            return;
        }

        faceVector(toTarget, 0.42f);
        ghost.getNavigation().stop();

        double distance = toTarget.length();
        if (distance > 1e-4) {
            Vec3 chargeVelocity = toTarget.scale(1.0 / distance).scale(Math.min(CHARGE_SPEED, distance));
            ghost.setDeltaMovement(chargeVelocity);
        }

        if (ghost.distanceToSqr(targetPos) < 3.0) {
            ghost.doHurtTarget(target);
            attackCooldown = (int) GhostEntityData.ATTACK_TICKS_INTERVAL;

            Vec3 look = ghost.getLookAngle();
            Vec3 backward = new Vec3(-look.x, 0, -look.z);
            backward = backward.lengthSqr() > 1e-4 ? backward.normalize() : new Vec3(0.6, 0, 0.0);

            retreatTarget = ghost.position().add(backward.scale(RETREAT_DISTANCE));

            ghost.getNavigation().stop();
            faceVector(toTarget, 1.0f);
            ghost.setDeltaMovement(backward.scale(0.45).add(0, 0.03, 0));
        }
    }

    private void faceVector(Vec3 vector, float yawLerp) {
        if (vector.horizontalDistanceSqr() <= 1e-4) {
            return;
        }

        float targetYaw = (float) Math.toDegrees(Mth.atan2(-vector.x, vector.z));
        ghost.setYRot(Mth.rotLerp(yawLerp, ghost.getYRot(), targetYaw));
        ghost.yHeadRot = ghost.yBodyRot = ghost.getYRot();
        ghost.yHeadRotO = ghost.yBodyRotO = ghost.getYRot();
    }
}
