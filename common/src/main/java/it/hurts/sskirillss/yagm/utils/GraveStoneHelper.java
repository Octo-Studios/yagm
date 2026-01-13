package it.hurts.sskirillss.yagm.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.UUID;


@SuppressWarnings("deprecation")
public class GraveStoneHelper {
    public static final UUID NULL_UUID = new UUID(0, 0);
    private static final int MAX_SEARCH_RADIUS = 3;
    private static final int MAX_SEARCH_HEIGHT = 2;

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos) {
        if (level.dimension() == Level.END && pos.getY() < 0) {
            return findEndPosition(level, pos);
        }

        if (isValidGravePosition(level, pos)) {
            return pos.immutable();
        }

        BlockPos fluidPos = findPositionAboveFluid(level, pos);
        if (fluidPos != null) {
            return fluidPos;
        }

        BlockPos airPos = findPositionInAir(level, pos);
        if (airPos != null) {
            return airPos;
        }

        return searchNearbyPosition(level, pos, MAX_SEARCH_RADIUS, MAX_SEARCH_HEIGHT);
    }

    private static BlockPos findEndPosition(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int radius = 0; radius <= 64; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) != radius && Math.abs(z) != radius) continue;

                    for (int y = 0; y < level.getMaxBuildHeight(); y++) {
                        mutable.set(pos.getX() + x, y, pos.getZ() + z);

                        BlockState block = level.getBlockState(mutable);
                        BlockState above = level.getBlockState(mutable.above());

                        if (block.isSolid() && (above.isAir() || above.canBeReplaced())) {
                            if (hasEnoughSpace(level, mutable.above(), 2)) {
                                return mutable.above().immutable();
                            }
                        }
                    }
                }
            }
        }
        return new BlockPos(0, 64, 0);
    }

    private static BlockPos findPositionAboveFluid(Level level, BlockPos startPos) {
        if (!level.getFluidState(startPos).isEmpty()) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(startPos.getX(), startPos.getY(), startPos.getZ());

            for (int y = startPos.getY(); y < level.getMaxBuildHeight(); y++) {
                mutable.setY(y);

                if (level.getFluidState(mutable).isEmpty() && level.getBlockState(mutable).isAir() && isValidBlockBelow(level, mutable.below())) {
                    return mutable.immutable();
                }
            }
        }
        return null;
    }

    private static BlockPos findPositionInAir(Level level, BlockPos startPos) {
        if (level.getBlockState(startPos).isAir()) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(startPos.getX(), startPos.getY(), startPos.getZ());

            for (int y = startPos.getY(); y >= level.getMinBuildHeight(); y--) {
                mutable.setY(y);

                BlockState current = level.getBlockState(mutable);
                BlockState below = level.getBlockState(mutable.below());

                if (current.isAir() && below.isSolid()) {
                    return mutable.immutable();
                }

                if (!level.getFluidState(mutable).isEmpty()) {
                    return findPositionAboveFluid(level, mutable.immutable());
                }
            }
        }
        return null;
    }

    private static BlockPos searchNearbyPosition(Level level, BlockPos center, int radius, int heightRange) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -heightRange; dy <= heightRange; dy++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    if (isValidGravePosition(level, mutable)) {
                        if (level.dimension() == Level.NETHER) {
                            if (!level.getFluidState(mutable.below()).isEmpty()) {
                                continue;
                            }
                        }
                        return mutable.immutable();
                    }
                }
            }
        }

        return center.immutable();
    }

    private static boolean isValidGravePosition(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());

        return (state.isAir() || state.canBeReplaced()) && below.isSolid() && level.getFluidState(pos).isEmpty() && hasEnoughSpace(level, pos, 1);
    }

    private static boolean isValidBlockBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos);
        return !below.isAir() && !level.getFluidState(pos).isEmpty();
    }

    private static boolean hasEnoughSpace(Level level, BlockPos pos, int height) {
        for (int i = 0; i < height; i++) {
            if (!level.getBlockState(pos.above(i)).isAir() && !level.getBlockState(pos.above(i)).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    public static boolean placeGraveStone(Level level, BlockPos pos, BlockState graveState) {
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return false;
        }

        if (tryPlaceAtPosition(level, pos, graveState)) {
            return true;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);

                    if (isValidGravePosition(level, mutable) && tryPlaceAtPosition(level, mutable, graveState)) {
                        return true;
                    }
                }
            }
        }

        return !level.isClientSide() && level.setBlock(pos, graveState, 3);
    }

    private static boolean tryPlaceAtPosition(Level level, BlockPos pos, BlockState graveState) {
        BlockState current = level.getBlockState(pos);
        if (current.isAir() || current.canBeReplaced()) {
            return level.setBlock(pos, graveState, 3);
        }
        return false;
    }
}