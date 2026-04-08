package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

public class GhostAggroGoal extends NearestAttackableTargetGoal<Player> {
    private final GhostEntity ghost;

    public GhostAggroGoal(GhostEntity ghost) {
        super(ghost, Player.class, true);
        this.ghost = ghost;
    }

    @Override
    public boolean canUse() {
        return !ghost.isTame() && isInAggroArea() && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (ghost.isTame()) {
            return false;
        }

        if (ghost.getTarget() instanceof Player player && (player.isCreative() || player.isSpectator())) {
            ghost.setTarget(null);
            return false;
        }

        return super.canContinueToUse();
    }

    private boolean isInAggroArea() {
        CemeteryManager manager = CemeteryManager.getInstance();
        BlockPos pos = ghost.blockPosition();
        int radius = (int) GhostEntityData.CEMETERY_AGGRO_RADIUS;

        return manager.isCemetery(ghost.level().dimension(), pos) || manager.getGraveCountNear(ghost.level().dimension(), pos, radius) > 0;
    }
}
