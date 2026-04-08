package it.hurts.sskirillss.yagm.block;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

@Getter
@AllArgsConstructor
public enum GraveStoneShape {

    TIER_1(
            Block.box(2, 0, 6, 14, 16, 10),
            Block.box(6, 0, 2, 10, 16, 14),
            null, null),

    TIER_2(
            Block.box(2, 0, 6, 14, 16, 10),
            Block.box(6, 0, 2, 10, 16, 14),
            null, null),

    TIER_3(
            Block.box(1, 0, 2, 15, 16, 15),
            Block.box(2, 0, 1, 15, 16, 15),
            Block.box(1, 0, 2, 15, 16, 15),
            Block.box(2, 0, 1, 15, 16, 15)),

    TIER_4(
            Block.box(1, 0, 1, 15, 16, 15),
            Block.box(1, 0, 1, 15, 16, 15),
            Block.box(1, 0, 1, 15, 16, 15),
            Block.box(1, 0, 1, 15, 16, 15));

    private final VoxelShape northSouth;
    private final VoxelShape eastWest;
    private final VoxelShape upperNorthSouth;
    private final VoxelShape upperEastWest;

    public boolean isDouble() {
        if (upperNorthSouth != null) {
            return !upperNorthSouth.isEmpty();
        }
        return false;
    }
}
