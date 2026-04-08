package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static net.minecraft.world.level.Level.NETHER;


@SuppressWarnings("deprecation")
public class PlaceableUtils {

    public static final UUID NULL_UUID = new UUID(0, 0);

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos) {
        if (!level.getFluidState(pos).isEmpty()) {
            return findFloorUnderFluid(level, pos);
        }

        if (isValidGravePosition(level, pos)) {
            return pos.immutable();
        }

        BlockPos airPos = findPosInAir(level, pos);
        if (airPos != null) {
            return airPos;
        }

        return searchNearbyPosition(level, pos, 3, 2);
    }

    private static BlockPos findFloorUnderFluid(Level level, BlockPos pos) {
        for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
            BlockPos candidate = new BlockPos(pos.getX(), y, pos.getZ());
            if (level.getBlockState(candidate.below()).isSolid()) {
                return candidate;
            }
        }
        return pos.immutable();
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
                        if (level.dimension() == NETHER) {
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

        return pPosition(level, pos, graveState);
    }


    public static boolean placeGraveStoneExact(Level level, BlockPos pos, BlockState graveState) {
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return false;
        }

        return pPosition(level, pos, graveState);
    }

    private static boolean pPosition(Level level, BlockPos pos, BlockState graveState) {
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

            boolean lowerWaterlogged = level.getFluidState(pos).isSourceOfType(Fluids.WATER);
            boolean upperWaterlogged = level.getFluidState(upperPos).isSourceOfType(Fluids.WATER);

            BlockState lowerState = graveState.setValue(GraveStoneBlock.HALF, DoubleBlockHalf.LOWER).setValue(GraveStoneBlock.WATERLOGGED, lowerWaterlogged);
            BlockState upperState = graveState.setValue(GraveStoneBlock.HALF, DoubleBlockHalf.UPPER).setValue(GraveStoneBlock.WATERLOGGED, upperWaterlogged);

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
    public static BlockPos findP2P(Level level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    mutable.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!isValidGravePosition(level, mutable)) continue;
                    if (level.dimension() == NETHER && !level.getFluidState(mutable.below()).isEmpty()) continue;
                    return mutable.immutable();
                }
            }
        }
        return null;
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
}
