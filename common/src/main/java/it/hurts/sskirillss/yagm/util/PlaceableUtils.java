package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static net.minecraft.world.level.Level.NETHER;

@SuppressWarnings("deprecation")
public class PlaceableUtils {

    public static final UUID NULL_UUID = new UUID(0, 0);
    private static final int SEARCH_RADIUS = 16;

    public static BlockPos getVoidRecovery(ServerLevel level, ServerPlayer player, @Nullable BlockPos trackedPos, GraveStoneLevels graveLevel) {
        int height = getGraveHeight(graveLevel);

        BlockPos origin = Objects.requireNonNullElseGet(trackedPos, player::blockPosition);
        BlockPos can = getGraveStoneBlockPosition(level, origin, height);

        if (isValidGravePosition(level, can, height)) {
            return can;
        }

        BlockPos columnPos = getColumn(level, origin, height);
        if (columnPos != null) {
            return columnPos;
        }

        return top(level, origin);
    }


    public static BlockPos getBedrockPlacement(ServerLevel level, BlockPos origin, int height) {
        BlockPos can = getColumn(level, origin, height);

        return can != null ? can : top(level, origin);
    }

    public static BlockPos getUnderBedrockPlacement(ServerLevel level, BlockPos origin, GraveStoneLevels graveLevel) {
        int height = getGraveHeight(graveLevel);

        BlockPos openBedrock = findNearestOpenBedrock(level, origin, height, SEARCH_RADIUS);

        if (openBedrock != null && level.setBlock(openBedrock, getBlockForLevel(level), 3)) {
            BlockPos gravePos = openBedrock.above();
            if (isValidGravePosition(level, gravePos, height)) {
                return gravePos;
            }
        }

        if (level.dimension() == NETHER) {
            BlockPos netherPos = findLowestNetherPosition(level, origin, height, SEARCH_RADIUS);

            return netherPos != null ? netherPos : top(level, origin);
        }

        BlockPos surfacePos = findVisibleSurface(level, origin, height, SEARCH_RADIUS);

        return surfacePos != null ? surfacePos : getBedrockPlacement(level, origin, height);
    }

    @Nullable
    private static BlockPos findLowestNetherPosition(ServerLevel level, BlockPos origin, int height, int radius) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = Math.min(level.getMaxBuildHeight(), level.getMinBuildHeight() + level.dimensionType().logicalHeight()) - height;

        int radiusSquared = radius * radius;

        for (int y = minY; y <= maxY; y++) {
            int bestDistance = Integer.MAX_VALUE;
            BlockPos best = null;

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int distance = dx * dx + dz * dz;

                    if (distance > radiusSquared || distance >= bestDistance) {
                        continue;
                    }

                    BlockPos candidate = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);

                    if (!level.hasChunkAt(candidate) || !isValidGravePosition(level, candidate, height)) {
                        continue;
                    }

                    bestDistance = distance;
                    best = candidate;
                }
            }

            if (best != null) {
                return best;
            }
        }

        return null;
    }

    @Nullable
    private static BlockPos findNearestOpenBedrock(ServerLevel level, BlockPos origin, int height, int radius) {
        int minY = level.getMinBuildHeight();
        int bestDistance = Integer.MAX_VALUE;

        BlockPos best = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distance = dx * dx + dz * dz;

                if (distance > radius * radius || distance >= bestDistance) {
                    continue;
                }

                BlockPos supportPos = new BlockPos(origin.getX() + dx, minY, origin.getZ() + dz);
                if (!level.hasChunkAt(supportPos)) {
                    continue;
                }

                BlockState supportState = level.getBlockState(supportPos);

                if (!(supportState.isAir() || supportState.canBeReplaced())) {
                    continue;
                }

                BlockPos gravePos = supportPos.above();
                if (!level.getFluidState(gravePos).isEmpty() || !hasEnoughSpace(level, gravePos, height)) {
                    continue;
                }

                bestDistance = distance;

                best = supportPos;
            }
        }

        return best;
    }

    @Nullable
    private static BlockPos findVisibleSurface(ServerLevel level, BlockPos origin, int height, int radius) {
        int bestGrassDistance = Integer.MAX_VALUE;
        int bestSurfaceDistance = Integer.MAX_VALUE;

        BlockPos bestGrass = null;
        BlockPos bestSurface = null;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {

                int distance = dx * dx + dz * dz;

                if (distance > radius * radius) {
                    continue;
                }

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                BlockPos columnPos = new BlockPos(x, level.getMinBuildHeight(), z);

                if (!level.hasChunkAt(columnPos)) {
                    continue;
                }

                int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                BlockPos candidate = new BlockPos(x, surfaceY, z);
                if (!isValidGravePosition(level, candidate, height)) {
                    continue;
                }

                if (distance < bestSurfaceDistance) {
                    bestSurfaceDistance = distance;
                    bestSurface = candidate;
                }

                if (level.getBlockState(candidate.below()).is(Blocks.GRASS_BLOCK) && distance < bestGrassDistance) {
                    bestGrassDistance = distance;
                    bestGrass = candidate;
                }
            }
        }

        return bestGrass != null ? bestGrass : bestSurface;
    }

    public static boolean isSafeStand(Level level, BlockPos pos) {
        return isValidGravePosition(level, pos);
    }

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos) {
        return getGraveStoneBlockPosition(level, pos, 1);
    }

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos, int height) {
        if (!level.getFluidState(pos).isEmpty()) {
            return inFluid(level, pos, height);
        }

        if (isValidGravePosition(level, pos, height)) {
            return pos.immutable();
        }

        BlockPos airPos = findPosInAir(level, pos, height);
        if (airPos != null) {
            return airPos;
        }

        BlockPos nearby = valid(level, pos, 3, 2, height);
        return nearby != null ? nearby : pos.immutable();
    }

    public static BlockPos top(ServerLevel level, BlockPos origin) {
        return new BlockPos(origin.getX(), level.getMinBuildHeight() + 1, origin.getZ());
    }

    @Nullable
    public static BlockPos getColumn(Level level, BlockPos origin, int height) {
        int minY = level.getMinBuildHeight() + 1;
        int maxY = level.getMaxBuildHeight() - height;

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(origin.getX(), minY, origin.getZ());

        for (int y = minY; y <= maxY; y++) {
            mutable.setY(y);
            if (isValidGravePosition(level, mutable, height)) {
                return mutable.immutable();
            }
        }

        return null;
    }

    private static BlockPos inFluid(Level level, BlockPos pos, int height) {
        for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos candidate = new BlockPos(pos.getX(), y, pos.getZ());
            if (isValidGravePosition(level, candidate, height)) {
                return candidate;
            }
        }
        return pos.immutable();
    }

    private static BlockPos aboveFluid(Level level, BlockPos startPos) {
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

    @Nullable
    private static BlockPos findPosInAir(Level level, BlockPos startPos, int height) {
        if (level.getBlockState(startPos).isAir()) {
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos(startPos.getX(), startPos.getY(), startPos.getZ());

            for (int y = startPos.getY(); y >= level.getMinBuildHeight(); y--) {
                mutable.setY(y);

                if (isValidGravePosition(level, mutable, height)) {
                    return mutable.immutable();
                }

                if (!level.getFluidState(mutable).isEmpty()) {
                    return aboveFluid(level, mutable.immutable());
                }
            }
        }
        return null;
    }


    @Nullable
    private static BlockPos valid(Level level, BlockPos center, int radius, int heightRange, int height) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -heightRange; dy <= heightRange; dy++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);

                    if (isValidGravePosition(level, mutable, height)) {
                        if (level.dimension() == NETHER && !level.getFluidState(mutable.below()).isEmpty()) {
                            continue;
                        }
                        return mutable.immutable();
                    }
                }
            }
        }

        return null;
    }

    private static boolean isValidGravePosition(Level level, BlockPos pos) {
        return isValidGravePosition(level, pos, 1);
    }

    private static boolean isValidGravePosition(Level level, BlockPos pos, int height) {
        BlockState state = level.getBlockState(pos);
        BlockState below = level.getBlockState(pos.below());

        return (state.isAir() || state.canBeReplaced()) && below.isSolid() && level.getFluidState(pos).isEmpty() && hasEnoughSpace(level, pos, height);
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
        return placeandGetPos(level, pos, graveState) != null;
    }

    @Nullable
    public static BlockPos placeandGetPos(Level level, BlockPos pos, BlockState graveState) {
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return null;
        }

        if (placeAt(level, pos, graveState)) {
            return pos.immutable();
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);

                    if (isValidGravePosition(level, mutable, getGraveHeight(graveState)) && placeAt(level, mutable, graveState)) {
                        return mutable.immutable();
                    }
                }
            }
        }

        return placeAt(level, pos, graveState) ? pos.immutable() : null;
    }


    public static boolean placeGraveExact(Level level, BlockPos pos, BlockState graveState) {
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return false;
        }

        return placeAt(level, pos, graveState);
    }

    private static boolean placeAt(Level level, BlockPos pos, BlockState graveState) {
        BlockState current = level.getBlockState(pos);
        if (!(current.isAir() || current.canBeReplaced())) {
            return false;
        }

        if (graveState.getBlock() instanceof GraveStoneBlock graveBlock && graveBlock.isDoubleShape()) {
            BlockPos upperPos = pos.above();
            if (upperPos.getY() >= level.getMaxBuildHeight()) {
                return false;
            }

            BlockState upperCurrent = level.getBlockState(upperPos);
            if (!(upperCurrent.isAir() || upperCurrent.canBeReplaced() || upperCurrent.is(Blocks.WATER))) {
                return false;
            }

            BlockState lowerState = graveState.setValue(GraveStoneBlock.HALF, DoubleBlockHalf.LOWER).setValue(GraveStoneBlock.WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER));
            BlockState upperState = graveState.setValue(GraveStoneBlock.HALF, DoubleBlockHalf.UPPER).setValue(GraveStoneBlock.WATERLOGGED, level.getFluidState(upperPos).isSourceOfType(Fluids.WATER));

            if (!level.setBlock(upperPos, upperState, 3)) {
                return false;
            }

            if (!level.setBlock(pos, lowerState, 3)) {
                level.removeBlock(upperPos, false);
                return false;
            }

            return true;
        }

        return level.setBlock(pos, graveState, 3);
    }

    @Nullable
    @Deprecated
    public static BlockPos findP2P(Level level, BlockPos center, int radius) {
        return getNear(level, center, radius);
    }

    @Nullable
    public static BlockPos getNear(Level level, BlockPos center, int radius) {
        return getNear(level, center, radius, 1);
    }

    @Nullable
    public static BlockPos getNear(Level level, BlockPos center, int radius, int height) {
        return valid(level, center, radius, radius, height);
    }


    public static BlockState getBlockForLevel(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            return Blocks.DIRT.defaultBlockState();
        }
        else if (level.dimension() == Level.NETHER) {
            return Blocks.NETHERRACK.defaultBlockState();
        }
        else if (level.dimension() == Level.END) {
            return Blocks.END_STONE.defaultBlockState();
        }

        return Blocks.DIRT.defaultBlockState();
    }

    public static int getGraveHeight(GraveStoneLevels graveLevel) {
        return graveLevel.getLevel() >= GraveStoneLevels.GRAVESTONE_LEVEL_3.getLevel() ? 2 : 1;
    }

    private static int getGraveHeight(BlockState graveState) {
        return graveState.getBlock() instanceof GraveStoneBlock graveBlock && graveBlock.isDoubleShape() ? 2 : 1;
    }
}
