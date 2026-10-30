package it.hurts.sskirillss.yagm.component.placement;

import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class GravePositionResolver {

    public static GravePlacementPlan resolveDeath(ServerPlayer player, GraveStoneLevels graveLevel) {
        ServerLevel level = player.serverLevel();
        DeathPlacementMode mode = setDeathMode(player);

        GravePlacementReason reason = switch (mode) {
            case VOID -> GravePlacementReason.VOID_RECOVERY;
            case UNDER_BEDROCK -> GravePlacementReason.UNDER_BEDROCK;
            case FALLING, IMMEDIATE -> GravePlacementReason.DEATH;
        };

        boolean recovery = isVoidRecovery(player);

        Vec3 spawnPos = player.position().add(0, 0.5, 0);
        Vec3 velocity = recovery ? Vec3.ZERO : FallingGraveMotionConfig.DEFAULT.randomLaunchVelocity(level.random);

        if (mode == DeathPlacementMode.FALLING && !ConfigRegistry.CONFIG.isFallingGraveEnabled()) {
            var graveHeight = PlaceableUtils.getGraveHeight(graveLevel);

            return new GravePlacementPlan(reason, DeathPlacementMode.IMMEDIATE, spawnPos, Vec3.ZERO, PlaceableUtils.groundBelow(level, player.blockPosition(), graveHeight), false, false);
        }

        if (mode == DeathPlacementMode.FALLING) {
            return new GravePlacementPlan(reason, mode, spawnPos, velocity, null, true, recovery);
        }

        boolean strictPlacement = level.dimension() != Level.END;

        BlockPos immediatePos;

        if (mode == DeathPlacementMode.UNDER_BEDROCK) {
            if (level.dimension() == Level.END) {
                immediatePos = PlaceableUtils.getBedrockPlacement(level, player.blockPosition(), PlaceableUtils.getGraveHeight(graveLevel));
            } else {
                immediatePos = PlaceableUtils.getUnderBedrockPlacement(level, player.blockPosition(), graveLevel);
            }
        } else {
            immediatePos = PlaceableUtils.top(level, player.blockPosition());
        }

        setImmediate(level, immediatePos);

        return new GravePlacementPlan(reason, mode, spawnPos, velocity, immediatePos, strictPlacement, true);
    }

    public static boolean isVoidRecovery(ServerPlayer player) {
        return player.level().dimension() == Level.END && isVoidDeath(player.level(), player.blockPosition());
    }

    public static BlockPos fallingLand(ServerLevel level, Vec3 position, boolean voidRecovery) {
        if (voidRecovery) {
            BlockPos base = BlockPos.containing(position);

            BlockPos supportPos = PlaceableUtils.top(level, base).below();

            if (ConfigRegistry.CONFIG.isCreateSupportBlock()) {
                level.setBlock(supportPos, PlaceableUtils.getBlockForLevel(level), 3);
            }

            return supportPos.above();
        }

        BlockPos blockPos = BlockPos.containing(position);

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

        return blockPos;
    }

    private static DeathPlacementMode setDeathMode(ServerPlayer player) {
        return getDeathMode(player.level(), player.blockPosition());
    }

    static DeathPlacementMode getDeathMode(Level level, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight()) {
            return DeathPlacementMode.UNDER_BEDROCK;
        }

        if (level.dimension() == Level.END && isVoidDeath(level, pos)) {
            return DeathPlacementMode.VOID;
        }

        return DeathPlacementMode.FALLING;
    }

    private static boolean isVoidDeath(Level level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight() + 2) {
            return true;
        }

        return isVoidColumn(level, pos);
    }

    private static boolean isVoidColumn(Level level, BlockPos origin) {
        int minY = level.getMinBuildHeight();
        int startY = Math.min(origin.getY(), level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), startY, origin.getZ());

        for (int y = startY; y > minY + 2; y--) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).isAir() || !level.getFluidState(cursor).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private static void setImmediate(ServerLevel level, BlockPos gravePos) {
        BlockPos supportPos = gravePos.below();
        BlockState supportState = level.getBlockState(supportPos);

        if (!ConfigRegistry.CONFIG.isCreateSupportBlock() || supportState.isSolid() || supportPos.getY() != level.getMinBuildHeight()) {
            return;
        }

        if (supportState.isAir() || supportState.canBeReplaced()) {
            level.setBlock(supportPos, PlaceableUtils.getBlockForLevel(level), 3);
        }
    }
}
