package it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.block.shape;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

@Getter
@AllArgsConstructor
public enum GraveStoneShape {
    TIER_1(Block.box(2, 0, 6, 14, 16, 10), Block.box(6, 0, 2, 10, 16, 14)),
    TIER_2(Block.box(2, 0, 6, 14, 16, 10), Block.box(6, 0, 2, 10, 16, 14)),
    TIER_3(Block.box(1, 0, 2, 15, 28, 15), Block.box(2, 0, 1, 15, 28, 15)),
    TIER_4(Block.box(1, 0, 1, 15, 32, 15), Block.box(1, 0, 1, 15, 32, 15));

    private final VoxelShape northSouth;
    private final VoxelShape eastWest;
}