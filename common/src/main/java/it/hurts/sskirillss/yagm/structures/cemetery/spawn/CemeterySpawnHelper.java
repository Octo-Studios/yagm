package it.hurts.sskirillss.yagm.structures.cemetery.spawn;


import it.hurts.sskirillss.yagm.structures.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structures.cemetery.data.CemeteryInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;

public final class CemeterySpawnHelper {

    private CemeterySpawnHelper() {}

    /**
     * Checks if the position is in a cemetery.
     * Uses CemeteryManager to determine grave clusters.
     *
     * @param level world
     * @param pos position to check
     * @return true if the position is in a cemetery
     */
    public static boolean isInCemetery(Level level, BlockPos pos) {
        return CemeteryManager.getInstance().isCemetery(level.dimension(), pos);
    }

    /**
     * Checks if the position is near a graveyard.
     *
     * @param level world
     * @param pos position
     * @param radius check radius
     * @return true if there is a graveyard within the radius
     */
    public static boolean isNearCemetery(Level level, BlockPos pos, int radius) {
        ResourceKey<Level> dimension = level.dimension();
        int graveCount = CemeteryManager.getInstance().getGraveCountNear(dimension, pos, radius);
        return graveCount >= CemeteryManager.getInstance().getMinGravesForCemetery();
    }

    /**
     * Gets the size of the cemetery (number of graves) at the given position.
     *
     * @param level world
     * @param pos position
     * @return the number of graves in the cluster, 0 if not a cemetery
     */
    public static int getCemeterySize(Level level, BlockPos pos) {
        return CemeteryManager.getInstance().getClusterSize(level.dimension(), pos);
    }

    /**
     * Gets the "power" of the cemetery to modify spawns.
     * The more graves, the stronger the effect.
     *
     * @param level world
     * @param pos position
     * @return power multiplier (0.0 - 1.0+)
     */
    public static float getCemeteryStrength(Level level, BlockPos pos) {
        int size = getCemeterySize(level, pos);
        if (size == 0) return 0f;

        int minGraves = CemeteryManager.getInstance().getMinGravesForCemetery();
        return (float) size / minGraves;
    }

    /**
     * Checks whether a special graveyard mob should spawn.
     * Takes into account the graveyard size, time of day, and randomness.
     *
     * @param level world
     * @param pos spawn position
     * @return true if a special mob can spawn
     */
    public static boolean shouldSpawnCemeteryMob(ServerLevel level, BlockPos pos) {
        if (!isInCemetery(level, pos)) {
            return false;
        }

        float strength = getCemeteryStrength(level, pos);
        float timeMultiplier = level.isNight() ? 1.5f : 0.5f;
        float chance = 0.1f * strength * timeMultiplier;

        return level.random.nextFloat() < chance;
    }


    /**
     * Gets the closest cemetery to the current position.
     *
     * @param level world
     * @param pos position
     * @param maxDistance maximum search distance
     * @return cemetery information or null
     */
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

    /**
     * Gets the center of the nearest graveyard.
     *
     * @param level world
     * @param pos position
     * @return the center of the graveyard or null
     */
    public static BlockPos getNearestCemeteryCenter(Level level, BlockPos pos) {
        if (isInCemetery(level, pos)) {
            return CemeteryManager.getInstance().getClusterCenter(level.dimension(), pos);
        }

        CemeteryInfo nearest = getNearestCemetery(level, pos, 128);
        return nearest != null ? nearest.getCenter() : null;
    }

    /**
     * Checks if this is a "large" graveyard.
     * Large graveyards can have special effects.
     *
     * @param level world
     * @param pos position
     * @param threshold threshold (number of graves)
     * @return true if the graveyard is large
     */
    public static boolean isLargeCemetery(Level level, BlockPos pos, int threshold) {
        return getCemeterySize(level, pos) >= threshold;
    }

    /**
     * Checks if this is an "ancient" cemetery.
     * TODO: Add tracking of grave creation times
     */
    public static boolean isAncientCemetery(Level level, BlockPos pos) {
        return isLargeCemetery(level, pos, 20);
    }
}