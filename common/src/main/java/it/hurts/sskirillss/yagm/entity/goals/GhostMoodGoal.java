package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.component.ghost_mode.GhostMood;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class GhostMoodGoal extends Goal {

    private final GhostEntity ghost;

    public GhostMoodGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        tickShyTimer();

        if (ghost.tickCount % GhostEntityData.MOOD_UPDATE_INTERVAL == 0) {
            updateMood();
        }
    }

    private void tickShyTimer() {
        int shy = ghost.getShyTimer();
        if (shy <= 0) return;

        ghost.setShyTimer(shy - 1);
        if (shy - 1 == 0 && ghost.isTame()) {
            ghost.updateMoodByBehavior();
        }
    }

    private void updateMood() {
        LivingEntity target = ghost.getTarget();

        if (ghost.isTame()) {
            if (target != null) {
                boolean clearTarget = !target.isAlive();
                Player owner = ghost.getOwner();
                if (!clearTarget && owner != null) {
                    double radius = Math.max(12.0, ghost.getAttributeValue(Attributes.FOLLOW_RANGE) * 2.0);
                    clearTarget = owner.distanceToSqr(target) > radius * radius;
                }
                if (clearTarget) {
                    ghost.setTarget(null);
                    target = null;
                }
            }

            if (ghost.getShyTimer() <= 0) {
                if (target != null && target.isAlive()) {
                    ghost.setMood(GhostMood.ANGRY);
                } else {
                    ghost.updateMoodByBehavior();
                }
            }
            return;
        }

        boolean validTarget = ghost.isValidUntamedTarget(target) && isTargetNearHome(target);
        if (!validTarget && target != null) {
            ghost.setTarget(null);
        }
        if (!validTarget) {
            ghost.setMood(GhostMood.DEFAULT);
            return;
        }

        ghost.setMood(GhostMood.ANGRY);
    }

    private boolean isTargetNearHome(LivingEntity target) {
        return ghost.isTargetNearHome(target);
    }
}
