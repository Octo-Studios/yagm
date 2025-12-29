package it.hurts.sskirillss.yagm.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.UUID;


@SuppressWarnings("all")
public class GraveStoneHelper {

    public static UUID NULL_UUID = new UUID(0, 0);

    public static BlockPos getGraveStoneBlockPosition(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());

        if (level.dimension() == Level.END && pos.getY() < 0) {
            for (int radius = 0; radius <= 64; radius++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) != radius && Math.abs(z) != radius) continue;

                        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
                            position.set(pos.getX() + x, y, pos.getZ() + z);
                            if (level.getBlockState(position).isSolid() && level.getBlockState(position.above()).isAir()) {
                                return position.above().immutable();
                            }
                        }
                    }
                }
            }
            return new BlockPos(0, 64, 0);
        }

        position.set(pos.getX(), pos.getY(), pos.getZ());
        FluidState fluid = level.getFluidState(position);

         if (!fluid.isEmpty()) {
            for (int y = pos.getY(); y < level.getMaxBuildHeight(); y++) {
                position.setY(y);
                if (level.getFluidState(position).isEmpty() && level.getBlockState(position).isAir()) {
                    BlockState below = level.getBlockState(position.below());
                    if (!below.isAir() || !level.getFluidState(position.below()).isEmpty()) {
                        return position.immutable();
                    }
                }
            }
        }


        if (level.getBlockState(position).isAir()) {
            for (int y = pos.getY(); y >= level.getMinBuildHeight(); y--) {
                position.setY(y);
                BlockState below = level.getBlockState(position.below());
                if (level.getBlockState(position).isAir() && below.isSolid()) {
                    return position.immutable();
                }
                if (!level.getFluidState(position).isEmpty()) {
                    return getGraveStoneBlockPosition(level, position.immutable());
                }
            }
        }


        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos check = position.offset(x, y, z);
                    BlockState state = level.getBlockState(check);
                    BlockState below = level.getBlockState(check.below());

                    if ((state.isAir() || state.canBeReplaced()) && below.isSolid() && level.getFluidState(check).isEmpty()) {
                        if (level.dimension() == Level.NETHER) {
                            FluidState fluidBelow = level.getFluidState(check.below());
                            if (!fluidBelow.isEmpty()) {
                                continue;
                            }
                        }
                        return check;
                    }
                }
            }
        }

        return position.immutable();
    }



    public static boolean placeGraveStone(Level level, BlockPos pos, BlockState graveState) {

        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))return false;


        BlockState current = level.getBlockState(pos);

        if (current.isAir() || current.canBeReplaced()) {
            boolean placed = level.setBlock(pos, graveState, 3);
            return placed;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos check = pos.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(check);
                    BlockState below = level.getBlockState(check.below());
                    if ((state.isAir() || state.canBeReplaced()) && below.isSolid() && level.getFluidState(check).isEmpty()) {
                        boolean placed = level.setBlock(check, graveState, 3);
                        if (placed) return true;
                    }
                }
            }
        }

        if (!level.isClientSide()) {
            return level.setBlock(pos, graveState, 3);
        }

        return false;
    }
}