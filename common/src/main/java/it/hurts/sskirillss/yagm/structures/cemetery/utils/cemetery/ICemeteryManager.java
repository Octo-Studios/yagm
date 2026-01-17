package it.hurts.sskirillss.yagm.structures.cemetery.utils.cemetery;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface ICemeteryManager {

    @FunctionalInterface
    interface CemeteryFormedCallback {
        void onFormed(ResourceKey<Level> dimension, BlockPos center, int graveCount);
    }

    @FunctionalInterface
    interface LevelChecker {
        Level getLevel(ResourceKey<Level> dimension);
    }


}
