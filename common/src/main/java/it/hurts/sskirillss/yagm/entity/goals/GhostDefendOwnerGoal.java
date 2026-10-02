package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

public class GhostDefendOwnerGoal extends TargetGoal {

    private final GhostEntity ghost;
    private LivingEntity attackTarget;
    private int LAST_HURT_TIME = -1;
    private int LAST_HURT_TIMESTAMP = -1;
    private boolean ownerAttackTimestampInitialized = false;

    public GhostDefendOwnerGoal(GhostEntity ghost) {
        super(ghost, false);
        this.ghost = ghost;
    }

    @Override
    public boolean canUse() {
        if (!ghost.isTame() || ghost.hasTameAttackDelay()) return false;

        Player owner = ghost.getOwner();
        if (owner == null) return false;

        LivingEntity currentTarget = ghost.getTarget();
        if (isValidTarget(currentTarget) && isNearOwner(owner, currentTarget)) return false;

        if (!ownerAttackTimestampInitialized) {
            LAST_HURT_TIME = owner.getLastHurtMobTimestamp();
            LAST_HURT_TIMESTAMP = owner.getLastHurtByMobTimestamp();
            ownerAttackTimestampInitialized = true;
            return false;
        }

        LivingEntity ownerAssistTarget = owner.getLastHurtMob();
        int ownerAssistTs = owner.getLastHurtMobTimestamp();
        if (ownerAssistTs != LAST_HURT_TIME && isValidTarget(ownerAssistTarget) && isNearOwner(owner, ownerAssistTarget)) {
            LAST_HURT_TIME = ownerAssistTs;
            attackTarget = ownerAssistTarget;
            return true;
        }

        LivingEntity ownerDefendTarget = owner.getLastHurtByMob();
        int ownerDefendTs = owner.getLastHurtByMobTimestamp();
        if (ownerDefendTs != LAST_HURT_TIMESTAMP && isValidTarget(ownerDefendTarget) && isNearOwner(owner, ownerDefendTarget)) {
            LAST_HURT_TIMESTAMP = ownerDefendTs;
            attackTarget = ownerDefendTarget;
            return true;
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        Player owner = ghost.getOwner();
        return owner != null && isValidTarget(attackTarget) && isNearOwner(owner, attackTarget) && super.canContinueToUse();
    }

    @Override
    public void start() {
        ghost.setTarget(attackTarget);
        super.start();
    }

    @Override
    public void stop() {
        attackTarget = null;
        super.stop();
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == ghost) {
            return false;
        }
        if (target instanceof GhostEntity) {
            return false;
        }

        return !(target instanceof Player player && ghost.isOwnedBy(player));
    }

    private boolean isNearOwner(Player owner, LivingEntity target) {
        double radius = Math.max(8.0, ghost.getAttributeValue(Attributes.FOLLOW_RANGE));
        return owner.distanceToSqr(target) <= radius * radius;
    }
}
