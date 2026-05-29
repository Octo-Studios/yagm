package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class GhostAggroGoal extends NearestAttackableTargetGoal<LivingEntity> {
    private final GhostEntity ghost;

    public GhostAggroGoal(GhostEntity ghost) {
        super(ghost, LivingEntity.class, 10, true, false, target -> isValidTarget(ghost, target));
        this.ghost = ghost;
    }

    @Override
    public boolean canUse() {
        LivingEntity currentTarget = ghost.getTarget();
        if (isValidTarget(ghost, currentTarget) && isTargetNearHome(currentTarget)) {
            return false;
        }

        return !ghost.isTame() && isInAggroArea() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (ghost.isTame()) {
            return false;
        }

        LivingEntity target = ghost.getTarget();
        if (!isValidTarget(ghost, target) || !isTargetNearHome(target)) {
            ghost.setTarget(null);
            return false;
        }

        return super.canContinueToUse();
    }

    private boolean isInAggroArea() {
        CemeteryManager manager = CemeteryManager.getInstance();
        BlockPos pos = ghost.getAnchorPos();
        int radius = (int) GhostEntityData.CEMETERY_AGGRO_RADIUS;

        return manager.isCemetery(ghost.level().dimension(), pos) || manager.getGraveCountNear(ghost.level().dimension(), pos, radius) > 0;
    }

    private static boolean isValidTarget(GhostEntity ghost, LivingEntity target) {
        if (target == null || !target.isAlive() || target == ghost) {
            return false;
        }

        if (target.getType().is(EntityTypeTags.UNDEAD)) {
            return false;
        }

        if (target instanceof Player player) {
            return !player.isCreative() && !player.isSpectator();
        }

        return true;
    }

    private boolean isTargetNearHome(LivingEntity target) {
        return ghost.isTargetNearHome(target);
    }
}
