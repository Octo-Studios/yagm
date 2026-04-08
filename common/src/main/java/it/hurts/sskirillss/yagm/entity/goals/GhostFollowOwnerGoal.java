package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class GhostFollowOwnerGoal extends Goal {

    private final GhostEntity ghost;
    private int wobbleTick = 0;

    public GhostFollowOwnerGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player owner = ghost.getOwner();
        return ghost.isTame() && ghost.getBehaviorMode() == BehaviorMode.FOLLOW && ghost.getTarget() == null && owner != null && !owner.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        Player owner = ghost.getOwner();
        if (owner == null) {
            return;
        }

        wobbleTick++;
        double distance = ghost.distanceTo(owner);

        if (distance > GhostEntityData.TELEPORT_DISTANCE) {
            ghost.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }

        double angle = (ghost.getId() % 6) * (Math.PI * 2.0 / 6.0);
        Vec3 orbitTarget = owner.position().add(Math.cos(angle) * GhostEntityData.FOLLOW_ORBIT_RADIUS, GhostEntityData.FOLLOW_ORBIT_HEIGHT, Math.sin(angle) * GhostEntityData.FOLLOW_ORBIT_RADIUS);

        Vec3 toOrbit = orbitTarget.subtract(ghost.position());
        double orbitDistance = toOrbit.length();

        if (orbitDistance < GhostEntityData.FOLLOW_ARRIVAL_DIST) {
            Vec3 bob = new Vec3(0, Math.sin(wobbleTick * GhostEntityData.BOB_SPEED) * GhostEntityData.BOB_AMPLITUDE, 0);
            ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.85).add(bob));
        } else {
            double speed = Math.min(orbitDistance * 0.04, GhostEntityData.FLY_SPEED * GhostEntityData.FOLLOW_SPEED_MULT);
            Vec3 perpendicular = new Vec3(-toOrbit.z, 0, toOrbit.x).normalize();
            double wobble = Math.sin(wobbleTick * GhostEntityData.WOBBLE_SPEED) * GhostEntityData.WOBBLE_AMPLITUDE;
            double bob = Math.sin(wobbleTick * GhostEntityData.BOB_SPEED) * GhostEntityData.BOB_AMPLITUDE;

            Vec3 desired = toOrbit.normalize().scale(speed).add(perpendicular.scale(wobble)).add(0, bob, 0);

            Vec3 velocity = ghost.getDeltaMovement().add(desired.scale(0.15));
            double currentSpeed = velocity.length();
            double max = GhostEntityData.FLY_SPEED * GhostEntityData.FOLLOW_SPEED_MULT;

            if (currentSpeed > max) {
                velocity = velocity.scale(max / currentSpeed);
            }

            ghost.setDeltaMovement(velocity);
        }

        Vec3 ownerFocusPos = owner.getEyePosition().add(getOwnerLookOffset());
        Vec3 toOwner = ownerFocusPos.subtract(ghost.getEyePosition());
        float lookYaw = (float) Math.toDegrees(Mth.atan2(toOwner.z, toOwner.x)) - 90f;
        ghost.setYRot(lookYaw);
        ghost.yHeadRot = lookYaw;
        ghost.yBodyRot = lookYaw;
        ghost.yHeadRotO = lookYaw;
        ghost.yBodyRotO = lookYaw;
    }
    private Vec3 getOwnerLookOffset() {
        double time = (ghost.tickCount + ghost.getId() * 13.0) * 0.08;
        double x = Math.sin(time) * 0.12;
        double y = Math.sin(time * 0.7 + 1.2) * 0.05;
        double z = Math.cos(time * 1.1) * 0.12;
        return new Vec3(x, y, z);
    }
}