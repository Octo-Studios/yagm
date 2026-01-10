package it.hurts.sskirillss.yagm.structures.cemetery;


import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.structures.cemetery.config.CemeteryConfig;
import it.hurts.sskirillss.yagm.structures.cemetery.data.CemeteryInfo;
import it.hurts.sskirillss.yagm.structures.cemetery.data.DimensionGraveData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.function.Function;


public class CemeteryManager {

    @Getter
    private static final CemeteryManager instance = new CemeteryManager();

    @Setter
    @Getter
    private int clusterRadius = CemeteryConfig.getDefaultRadius();

    @Setter
    @Getter
    private int minGravesForCemetery = CemeteryConfig.getDefaultMinGraves();

    private final Map<ResourceKey<Level>, DimensionGraveData> dimensions = new HashMap<>();

    @Setter
    private CemeteryFormedCallback onCemeteryFormed;

    private CemeteryManager() {}

    @FunctionalInterface
    public interface CemeteryFormedCallback {
        void onFormed(ResourceKey<Level> dimension, BlockPos center, int graveCount);
    }


    /**
     * Add Grave
     */
    public void addGrave(ResourceKey<Level> dimension, BlockPos pos) {
        getData(dimension).addGrave(pos);
    }

    /**
     * Remove Grave
     */
    public void removeGrave(ResourceKey<Level> dimension, BlockPos pos) {
        getData(dimension).removeGrave(pos);
    }

    /**
     * Checks if a position is part of a cemetery
     */
    public boolean isCemetery(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).isCemetery(pos);
    }

    /**
     * Cluster Size
     */
    public int getClusterSize(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).getClusterSize(pos);
    }

    /**
     * All graves in the cluster
     */
    public Set<BlockPos> getClusterGraves(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).getClusterGraves(pos);
    }

    /**
     * Cluster center
     */
    public BlockPos getClusterCenter(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).getClusterCenter(pos);
    }

    /**
     * Graves within radius
     */
    public Set<BlockPos> getGravesInRadius(ResourceKey<Level> dimension, BlockPos pos, int radius) {
        return getData(dimension).getGravesInRadius(pos, radius);
    }

    /**
     * Number of graves nearby
     */
    public int getGraveCountNear(ResourceKey<Level> dimension, BlockPos pos, int radius) {
        return getData(dimension).getGraveCountNear(pos, radius);
    }

    /**
     * All cemeteries in the dimension
     */
    public List<CemeteryInfo> getAllCemeteries(ResourceKey<Level> dimension) {
        return getData(dimension).getAllCemeteries();
    }

    /**
     * All graves in the dimension
     */
    public Set<BlockPos> getAllGraves(ResourceKey<Level> dimension) {
        return getData(dimension).getAllGraves();
    }

    /**
     * Number of graves in the dimension
     */
    public int getGraveCount(ResourceKey<Level> dimension) {
        return getData(dimension).getGraveCount();
    }

    /**
     * Contains a grave
     */
    public boolean containsGrave(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).containsGrave(pos);
    }


    public void clearDimension(ResourceKey<Level> dimension) {
        dimensions.remove(dimension);
    }

    public void clear() {
        dimensions.clear();
    }

    public Set<ResourceKey<Level>> getLoadedDimensions() {
        return Collections.unmodifiableSet(dimensions.keySet());
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        for (Map.Entry<ResourceKey<Level>, DimensionGraveData> entry : dimensions.entrySet()) {
            String key = entry.getKey().location().toString();
            tag.put(key, entry.getValue().save());
        }

        return tag;
    }

    public void load(CompoundTag tag, Function<String, ResourceKey<Level>> dimensionResolver) {
        dimensions.clear();

        for (String key : tag.getAllKeys()) {
            ResourceKey<Level> dimension = dimensionResolver.apply(key);
            if (dimension != null) {
                DimensionGraveData data = createDimensionData();
                data.load(tag.getCompound(key));
                dimensions.put(dimension, data);

                YAGMCommon.LOGGER.debug("Loaded {} graves for dimension {}", data.getGraveCount(), key);
            }
        }
    }

    private DimensionGraveData getData(ResourceKey<Level> dimension) {
        return dimensions.computeIfAbsent(dimension, k -> {
            DimensionGraveData data = createDimensionData();

            data.setOnCemeteryFormed((center, size) -> {
                YAGMCommon.LOGGER.info("Cemetery formed at {} with {} graves in {}", center, size, dimension.location());

                if (onCemeteryFormed != null) {
                    onCemeteryFormed.onFormed(dimension, center, size);
                }
            });

            return data;
        });
    }

    private DimensionGraveData createDimensionData() {
        return new DimensionGraveData(clusterRadius, minGravesForCemetery);
    }
}