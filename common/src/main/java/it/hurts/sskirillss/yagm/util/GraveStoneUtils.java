package it.hurts.sskirillss.yagm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.UUID;


@SuppressWarnings("deprecation")
public class GraveStoneUtils {
    public static final UUID NULL_UUID = new UUID(0, 0);
    private static final int MAX_SEARCH_RADIUS = 3;
    private static final int MAX_SEARCH_HEIGHT = 2;

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos) {
        if (level.dimension() == Level.END && pos.getY() < 0) {
            return findEndPos(level, pos);
        }

        if (isValidGravePosition(level, pos)) {
            return pos.immutable();
        }

        BlockPos fluidPos = findPosAboveFluid(level, pos);
        if (fluidPos != null) {
            return fluidPos;
        }

        BlockPos airPos = findPosInAir(level, pos);
        if (airPos != null) {
            return airPos;
        }

        return searchNearbyPosition(level, pos, MAX_SEARCH_RADIUS, MAX_SEARCH_HEIGHT);
    }

    private static BlockPos findEndPos(Level level, BlockPos pos) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int radius = 0; radius <= 64; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;

                    int wx = pos.getX() + x;
                    int wz = pos.getZ() + z;

                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
                    if (surfaceY <= level.getMinBuildHeight()) continue;

                    BlockPos blockPos = new BlockPos(wx, surfaceY, wz);
                    if (hasEnoughSpace(level, blockPos, 2)) {
                        double distance = Math.sqrt((double) x * x + (double) (surfaceY - pos.getY()) * (surfaceY - pos.getY()) + (double) z * z);

                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestPos = blockPos;
                            if (distance < 16) return bestPos;
                        }
                    }
                }
            }
            if (bestPos != null && radius > 16) return bestPos;
        }

        if (bestPos == null) {
            int mainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            if (mainY > level.getMinBuildHeight()) {
                bestPos = new BlockPos(0, mainY, 0);
            }
        }

        return bestPos != null ? bestPos : new BlockPos(0, 65, 0);
    }

    private static BlockPos findPosAboveFluid(Level level, BlockPos startPos) {
        if (!level.getFluidState(startPos).isEmpty()) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(startPos.getX(), startPos.getY(), startPos.getZ());

            for (int y = startPos.getY(); y < level.getMaxBuildHeight(); y++) {
                mutable.setY(y);

                if (level.getFluidState(mutable).isEmpty() && level.getBlockState(mutable).isAir() && isFluidBelow(level, mutable.below())) {
                    return mutable.immutable();
                }
            }
        }
        return null;
    }

    private static BlockPos findPosInAir(Level level, BlockPos startPos) {
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
                    return findPosAboveFluid(level, mutable.immutable());
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


    private static boolean isFluidBelow(Level level, BlockPos pos) {
        return !level.getBlockState(pos).isAir() && !level.getFluidState(pos).isEmpty();
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

        if (pPosition(level, pos, graveState)) {
            return true;
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);

                    if (isValidGravePosition(level, mutable) && pPosition(level, mutable, graveState)) {
                        return true;
                    }
                }
            }
        }

        return !level.isClientSide() && level.setBlock(pos, graveState, 3);
    }

    private static boolean pPosition(Level level, BlockPos pos, BlockState graveState) {
        BlockState current = level.getBlockState(pos);
        if (current.isAir() || current.canBeReplaced()) {
            return level.setBlock(pos, graveState, 3);
        }
        return false;
    }
}
