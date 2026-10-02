package it.hurts.sskirillss.yagm.component.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record GravePlacementResult(boolean placed, @Nullable BlockPos pos, @Nullable BlockState state) {
    public static GravePlacementResult failed() {
        return new GravePlacementResult(false, null, null);
    }

    public static GravePlacementResult placed(BlockPos pos, BlockState state) {
        return new GravePlacementResult(true, pos.immutable(), state);
    }
}
