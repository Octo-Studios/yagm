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
    private int attackCooldown = 0;

    public GhostSnippedAttackGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = ghost.getTarget();
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
    }

    @Override
    public void tick() {
        LivingEntity target = ghost.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        double bob = Math.sin((ghost.tickCount + ghost.getId() * 11.0) * 0.22) * 0.14;
        Vec3 targetPos = target.position().add(0, target.getEyeHeight() * 0.5 + bob, 0);
        Vec3 toTarget = targetPos.subtract(ghost.position());

        if (attackCooldown > 0) {
            attackCooldown--;
            ghost.getNavigation().stop();

            Vec3 look = ghost.getLookAngle();
            Vec3 backward = new Vec3(-look.x, 0, -look.z);

            if (backward.lengthSqr() < 1e-4) {
                Vec3 away = ghost.position().subtract(target.position());
                backward = new Vec3(away.x, 0, away.z);
            }

            if (backward.lengthSqr() < 1e-4) {
                backward = new Vec3(1, 0, 0);
            }

            backward = backward.normalize();
            double retreatBob = Math.sin((ghost.tickCount + ghost.getId() * 7.0) * 0.25) * 0.05;
            Vec3 desired = backward.scale(0.65).add(0, 0.04 + retreatBob, 0);
            Vec3 velocity = ghost.getDeltaMovement().scale(0.45).add(desired.scale(0.55));
            ghost.setDeltaMovement(velocity);
            return;
        }

        faceVector(toTarget, 0.42f);
        ghost.getNavigation().moveTo(targetPos.x, targetPos.y, targetPos.z, 1.75);

        if (ghost.distanceToSqr(targetPos) < 3.0) {
            ghost.doHurtTarget(target);
            attackCooldown = (int) GhostEntityData.ATTACK_TICKS_INTERVAL;

            Vec3 look = ghost.getLookAngle();
            Vec3 backward = new Vec3(-look.x, 0, -look.z);
            if (backward.lengthSqr() > 1e-4) {
                backward = backward.normalize();
            } else {
                backward = new Vec3(0.6, 0, 0.0);
            }

            ghost.getNavigation().stop();
            ghost.setDeltaMovement(backward.scale(0.45).add(0, 0.05, 0));
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