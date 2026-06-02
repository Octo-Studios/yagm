package it.hurts.sskirillss.yagm.entity.goals;

import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class GhostBreedGoal extends Goal {
    private static final int MIN_APPROACH_TICKS = 10;
    private static final double BREED_DISTANCE_SQR = 2.25D;
    private final GhostEntity ghost;
    private GhostEntity partner;
    private int loveTime;

    public GhostBreedGoal(GhostEntity ghost) {
        this.ghost = ghost;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ghost.isInLove() || ghost.isBaby()) {
            return false;
        }

        partner = findPartner();
        return partner != null;
    }

    @Override
    public boolean canContinueToUse() {
        return partner != null && partner.isAlive() && ghost.canBreedWith(partner) && loveTime < 60;
    }

    @Override
    public void stop() {
        partner = null;
        loveTime = 0;
    }

    @Override
    public void tick() {
        if (partner == null) {
            return;
        }

        ghost.getLookControl().setLookAt(partner, 10.0f, ghost.getMaxHeadXRot());
        partner.getLookControl().setLookAt(ghost, 10.0f, partner.getMaxHeadXRot());
        ghost.getNavigation().moveTo(partner, 1.0D);

        loveTime++;
        if (loveTime >= MIN_APPROACH_TICKS && ghost.distanceToSqr(partner) <= BREED_DISTANCE_SQR) {
            breed();
        }
    }

    private @Nullable GhostEntity findPartner() {
        GhostEntity nearest = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (GhostEntity candidate : ghost.level().getEntitiesOfClass(GhostEntity.class, ghost.getBoundingBox().inflate(8.0D), ghost::canBreedWith)) {
            double distanceSqr = ghost.distanceToSqr(candidate);
            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private void breed() {
        if (!(ghost.level() instanceof ServerLevel serverLevel) || partner == null) {
            return;
        }

        AgeableMob offspring = ghost.getBreedOffspring(serverLevel, partner);
        if (!(offspring instanceof GhostEntity child)) {
            return;
        }

        Player loveCause = ghost.getLoveCause();
        if (loveCause == null) {
            loveCause = partner.getLoveCause();
        }

        Vec3 childPos = ghost.position().add(partner.position()).scale(0.5D).add(0.0D, 0.2D, 0.0D);
        child.moveTo(childPos.x, childPos.y, childPos.z, ghost.getYRot(), ghost.getXRot());
        child.setBaby(true);

        if (!serverLevel.addFreshEntity(child)) {
            return;
        }

        ghost.setAge(GhostEntityData.BREED_COOLDOWN_TICKS);
        partner.setAge(GhostEntityData.BREED_COOLDOWN_TICKS);
        ghost.resetLove();
        partner.resetLove();

        ghost.spawnHeartParticles(7);
        partner.spawnHeartParticles(7);
        child.spawnHeartParticles(7);

        if (serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            ExperienceOrb.award(serverLevel, childPos, Mth.nextInt(ghost.getRandom(), 1, 7));
        }

        if (loveCause instanceof ServerPlayer serverPlayer) {
            serverPlayer.awardStat(Stats.ANIMALS_BRED);
        }

        stop();
    }
}
