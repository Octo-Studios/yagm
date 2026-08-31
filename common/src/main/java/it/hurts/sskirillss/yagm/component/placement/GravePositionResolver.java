package it.hurts.sskirillss.yagm.component.placement;

import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class GravePositionResolver {

    public static GravePlacementPlan resolveDeath(ServerPlayer player, @Nullable Vec3 trackedSpawnPos, @Nullable BlockPos trackedPlacementPos) {
        ServerLevel level = player.serverLevel();
        DeathPlacementMode mode = setDeathMode(player);

        GravePlacementReason reason = switch (mode) {
            case VOID -> GravePlacementReason.VOID_RECOVERY;
            case UNDER_BEDROCK -> GravePlacementReason.UNDER_BEDROCK;
            case FALLING -> GravePlacementReason.DEATH;
        };

        boolean recovery = isVoidRecovery(player);

        Vec3 spawnPos = recovery && trackedSpawnPos != null ? trackedSpawnPos : player.position().add(0, 0.5, 0);
        Vec3 velocity = recovery ? Vec3.ZERO : FallingGraveMotionConfig.DEFAULT.randomLaunchVelocity(level.random);

        if (mode == DeathPlacementMode.FALLING) {
            return new GravePlacementPlan(reason, mode, spawnPos, velocity, null, true, recovery);
        }

        boolean strictPlacement = mode != DeathPlacementMode.VOID;
        BlockPos immediatePos = mode == DeathPlacementMode.UNDER_BEDROCK ? PlaceableUtils.getBedrockPlacement(level, player.blockPosition()) : PlaceableUtils.getVoidRecovery(level, player, trackedPlacementPos);

        setImmediate(level, immediatePos);

        return new GravePlacementPlan(reason, mode, spawnPos, velocity, immediatePos, strictPlacement, true);
    }

    public static boolean canTrackSafeStand(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }

        Level level = player.level();
        BlockPos pos = player.blockPosition();

        return pos.getY() > level.getMinBuildHeight() + 2 && PlaceableUtils.isSafeStand(level, pos);
    }

    public static boolean isVoidRecovery(ServerPlayer player) {
        return player.level().dimension() == Level.END && player.blockPosition().getY() < player.level().getMinBuildHeight();
    }

    public static BlockPos resolveFallingLanding(ServerLevel level, Vec3 position, @Nullable BlockPos lastSafePos, boolean voidRecovery) {
        if (voidRecovery) {
            BlockPos base = lastSafePos != null ? lastSafePos : BlockPos.containing(position);

            BlockPos supportPos = new BlockPos(base.getX(), level.getMinBuildHeight(), base.getZ());

            level.setBlock(supportPos, PlaceableUtils.getBlockForLevel(level), 3);

            return supportPos.above();
        }

        BlockPos blockPos = BlockPos.containing(position);

        if (blockPos.getY() <= level.getMinBuildHeight() + 4 && lastSafePos != null) {
            blockPos = lastSafePos;
        }

        if (blockPos.getY() <= level.getMinBuildHeight() + 2) {
            return new BlockPos(blockPos.getX(), level.getMinBuildHeight() + 1, blockPos.getZ());
        }

        BlockState state = level.getBlockState(blockPos);
        if (!state.isAir() && !state.canBeReplaced()) {
            for (int y = 0; y <= 5; y++) {
                BlockPos above = blockPos.above(y);
                BlockState aboveState = level.getBlockState(above);
                BlockState below = level.getBlockState(above.below());

                if ((aboveState.isAir() || aboveState.canBeReplaced()) && below.isSolid()) {
                    return above;
                }
            }
        }

        for (int y = 0; y <= 10; y++) {
            BlockPos below = blockPos.below(y);
            BlockState belowState = level.getBlockState(below);
            BlockState atPos = level.getBlockState(below.above());

            if (belowState.isSolid() && (atPos.isAir() || atPos.canBeReplaced())) {
                return below.above();
            }
        }

        return blockPos;
    }

    private static DeathPlacementMode setDeathMode(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (pos.getY() < level.getMinBuildHeight()) {
            return level.dimension() == Level.END ? DeathPlacementMode.VOID : DeathPlacementMode.UNDER_BEDROCK;
        }

        return DeathPlacementMode.FALLING;
    }

    private static void setImmediate(ServerLevel level, BlockPos gravePos) {
        BlockPos supportPos = gravePos.below();
        BlockState supportState = level.getBlockState(supportPos);

        if (supportState.isSolid() || supportPos.getY() != level.getMinBuildHeight()) {
            return;
        }

        if (supportState.isAir() || supportState.canBeReplaced()) {
            level.setBlock(supportPos, PlaceableUtils.getBlockForLevel(level), 3);
        }
    }
}
