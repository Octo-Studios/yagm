package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;

public class GhostDefendOwnerGoal extends TargetGoal {

    private final GhostEntity ghost;
    private LivingEntity attackTarget;

    public GhostDefendOwnerGoal(GhostEntity ghost) {
        super(ghost, false);
        this.ghost = ghost;
    }

    @Override
    public boolean canUse() {
        if (!ghost.isTame()) {
            return false;
        }

        Player owner = ghost.getOwner();
        if (owner == null) {
            return false;
        }

        LivingEntity target = getOwnerAttackTarget(owner);
        if (!isValidTarget(target)) {
            return false;
        }

        attackTarget = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return isValidTarget(attackTarget) && super.canContinueToUse();
    }

    @Override
    public void start() {
        ghost.setTarget(attackTarget);
        super.start();
    }

    private LivingEntity getOwnerAttackTarget(Player owner) {
        LivingEntity target = owner.getLastHurtByMob();
        if (isValidTarget(target)) {
            return target;
        }

        return owner.getLastHurtMob();
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null || !target.isAlive() || target == ghost) {
            return false;
        }

        return !(target instanceof Player player && ghost.isOwnedBy(player));
    }
}
