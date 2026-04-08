package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class GhostMoveControl extends MoveControl {

    private static final double ACCEL = 0.075;
    private static final double MAX_SPEED = 0.96;

    private final GhostEntity ghost;

    public GhostMoveControl(GhostEntity ghost) {
        super(ghost);
        this.ghost = ghost;
    }

    @Override
    public void tick() {
        if (operation != Operation.MOVE_TO) {
            return;
        }

        Vec3 toTarget = new Vec3(wantedX, wantedY, wantedZ).subtract(ghost.position());
        double distance = toTarget.length();

        if (distance < ghost.getBoundingBox().getSize()) {
            operation = Operation.WAIT;
            ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.5));
            return;
        }

        Vec3 acceleration = toTarget.normalize().scale(speedModifier * 0.075);
        Vec3 velocity = ghost.getDeltaMovement().add(acceleration);

        double speed = velocity.length();
        double max = 0.96 * speedModifier;
        if (speed > max) {
            velocity = velocity.scale(max / speed);
        }

        ghost.setDeltaMovement(velocity);

        Player owner = ghost.getOwner();

        boolean lockOwnerLook = ghost.isTame() && ghost.getBehaviorMode() == BehaviorMode.FOLLOW && ghost.getTarget() == null && owner != null && !owner.isSpectator();

        if (lockOwnerLook) {
            return;
        }

        if (ghost.getTarget() != null) {
            double dx = ghost.getTarget().getX() - ghost.getX();
            double dz = ghost.getTarget().getZ() - ghost.getZ();
            ghost.setYRot(-((float) Mth.atan2(dx, dz)) * (180f / (float) Math.PI));
        } else if (velocity.horizontalDistanceSqr() > 1e-6) {
            ghost.setYRot(-((float) Mth.atan2(velocity.x, velocity.z)) * (180f / (float) Math.PI));
        }

        ghost.yBodyRot = ghost.getYRot();
    }
}
