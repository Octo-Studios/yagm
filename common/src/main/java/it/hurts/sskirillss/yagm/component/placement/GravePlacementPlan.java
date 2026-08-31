package it.hurts.sskirillss.yagm.component.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record GravePlacementPlan(GravePlacementReason reason, DeathPlacementMode mode, Vec3 spawnPos, Vec3 velocity, @Nullable BlockPos immediatePos, boolean strictPlacement, boolean recovery) {
    public boolean falling() {
        return mode == DeathPlacementMode.FALLING;
    }
}
