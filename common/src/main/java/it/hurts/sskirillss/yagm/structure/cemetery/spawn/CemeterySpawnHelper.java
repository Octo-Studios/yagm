package it.hurts.sskirillss.yagm.structure.cemetery.spawn;


import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeteryInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;

public final class CemeterySpawnHelper {


    public static boolean isInCemetery(Level level, BlockPos pos) {
        return CemeteryManager.getInstance().isCemetery(level.dimension(), pos);
    }


    public static boolean isNearCemetery(Level level, BlockPos pos, int radius) {
        ResourceKey<Level> dimension = level.dimension();
        int graveCount = CemeteryManager.getInstance().getGraveCountNear(dimension, pos, radius);
        return graveCount >= CemeteryManager.getInstance().getMinGravesForCemetery();
    }


    public static int getCemeterySize(Level level, BlockPos pos) {
        return CemeteryManager.getInstance().getClusterSize(level.dimension(), pos);
    }


    public static float getCemeteryStrength(Level level, BlockPos pos) {
        int size = getCemeterySize(level, pos);
        if (size == 0) return 0f;

        int minGraves = CemeteryManager.getInstance().getMinGravesForCemetery();
        return (float) size / minGraves;
    }


    public static boolean shouldSpawnCemeteryMob(ServerLevel level, BlockPos pos) {
        if (!isInCemetery(level, pos)) {
            return false;
        }

        float strength = getCemeteryStrength(level, pos);
        float timeMultiplier = level.isNight() ? 1.5f : 0.5f;
        float chance = 0.1f * strength * timeMultiplier;

        return level.random.nextFloat() < chance;
    }


    public static CemeteryInfo getNearestCemetery(Level level, BlockPos pos, int maxDistance) {
        List<CemeteryInfo> cemeteries = CemeteryManager.getInstance().getAllCemeteries(level.dimension());

        CemeteryInfo nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (CemeteryInfo cemetery : cemeteries) {
            double dist = cemetery.distanceTo(pos);
            if (dist < nearestDist && dist <= maxDistance) {
                nearestDist = dist;
                nearest = cemetery;
            }
        }

        return nearest;
    }


    public static BlockPos getNearestCemeteryCenter(Level level, BlockPos pos) {
        if (isInCemetery(level, pos)) {
            return CemeteryManager.getInstance().getClusterCenter(level.dimension(), pos);
        }

        CemeteryInfo nearest = getNearestCemetery(level, pos, 128);
        return nearest != null ? nearest.getCenter() : null;
    }


    public static boolean isLargeCemetery(Level level, BlockPos pos, int threshold) {
        return getCemeterySize(level, pos) >= threshold;
    }


    public static boolean isAncientCemetery(Level level, BlockPos pos) {
        return isLargeCemetery(level, pos, 20);
    }
}