package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

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
        return ghost.isTame() && !ghost.isInLove() && ghost.getBehaviorMode() == BehaviorMode.FOLLOW && ghost.getTarget() == null && owner != null && !owner.isSpectator();
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
        UUID ownerUuid = owner.getUUID();
        double distance = ghost.distanceTo(owner);

        if (distance > GhostEntityData.TELEPORT_DISTANCE) {
            ghost.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }

        List<GhostEntity> orbitGhosts = ghost.level().getEntitiesOfClass(GhostEntity.class, owner.getBoundingBox().inflate(14.0D), other -> other.isTame() && other.getBehaviorMode() == BehaviorMode.FOLLOW && other.getTarget() == null && ownerUuid.equals(other.getOwnerUUID()));
        orbitGhosts.sort(Comparator.comparingInt(GhostEntity::getId));

        int slot = orbitGhosts.indexOf(ghost);
        if (slot < 0) {
            orbitGhosts.add(ghost);
            orbitGhosts.sort(Comparator.comparingInt(GhostEntity::getId));
            slot = orbitGhosts.indexOf(ghost);
        }

        int count = Math.max(1, orbitGhosts.size());
        double angle = wobbleTick * 0.03D + slot * (Math.PI * 2.0D / count);

        double ringOffset = count > 7 ? ((slot % 2 == 0) ? 0.3D : -0.3D) : 0.0D;
        double heightOffset = count > 5 ? ((slot % 2 == 0) ? 0.15D : -0.15D) : 0.0D;
        double orbitRadius = GhostEntityData.FOLLOW_ORBIT_RADIUS + ringOffset;
        Vec3 orbitTarget = owner.position().add(Math.cos(angle) * orbitRadius, GhostEntityData.FOLLOW_ORBIT_HEIGHT + heightOffset, Math.sin(angle) * orbitRadius);
        Vec3 toOrbit = orbitTarget.subtract(ghost.position());
        double orbitDistance = toOrbit.length();

        if (orbitDistance < GhostEntityData.FOLLOW_ARRIVAL_DIST) {
            double yDiff = orbitTarget.y - ghost.getY();
            Vec3 cur = ghost.getDeltaMovement();
            Vec3 velocity = new Vec3(cur.x * 0.8, cur.y * 0.8 + yDiff * 0.05, cur.z * 0.8);
            ghost.setDeltaMovement(velocity);
        } else {
            double speed = Math.min(orbitDistance * 0.04D, GhostEntityData.FLY_SPEED * GhostEntityData.FOLLOW_SPEED_MULT);
            Vec3 perpendicular = new Vec3(-toOrbit.z, 0, toOrbit.x).normalize();
            double wobble = Math.sin(wobbleTick * GhostEntityData.WOBBLE_SPEED) * GhostEntityData.WOBBLE_AMPLITUDE;
            double bob = Math.sin(wobbleTick * GhostEntityData.BOB_SPEED) * GhostEntityData.BOB_AMPLITUDE;

            Vec3 desired = toOrbit.normalize().scale(speed).add(perpendicular.scale(wobble)).add(0, bob, 0);
            Vec3 velocity = ghost.getDeltaMovement().add(desired.scale(0.15D));
            double currentSpeed = velocity.length();
            double max = GhostEntityData.FLY_SPEED * GhostEntityData.FOLLOW_SPEED_MULT;

            if (currentSpeed > max) {
                velocity = velocity.scale(max / currentSpeed);
            }

            ghost.setDeltaMovement(velocity);
        }

        Vec3 ownerFocus = owner.getEyePosition().add(ghost.getOwnerLookOffset()).subtract(ghost.getEyePosition());
        float lookYaw = (float) Math.toDegrees(Math.atan2(ownerFocus.z, ownerFocus.x)) - 90f;
        ghost.setYRot(lookYaw);
        ghost.yHeadRot = ghost.yBodyRot = lookYaw;
        ghost.yHeadRotO = ghost.yBodyRotO = lookYaw;
    }
}
