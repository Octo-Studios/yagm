package it.hurts.sskirillss.yagm.structures.cemetery;


import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.structures.cemetery.config.CemeteryConfig;
import it.hurts.sskirillss.yagm.structures.cemetery.data.CemeteryInfo;
import it.hurts.sskirillss.yagm.structures.cemetery.data.CemeterySavedData;
import it.hurts.sskirillss.yagm.structures.cemetery.data.DimensionGraveData;
import it.hurts.sskirillss.yagm.structures.cemetery.utils.cemetery.ICemeteryManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

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

    private ICemeteryManager.CemeteryFormedCallback onCemeteryFormed;
    
    public void setOnCemeteryFormed(ICemeteryManager.CemeteryFormedCallback callback) {
        this.onCemeteryFormed = callback;
        for (Map.Entry<ResourceKey<Level>, DimensionGraveData> entry : dimensions.entrySet()) {
            ResourceKey<Level> dimension = entry.getKey();
            DimensionGraveData data = entry.getValue();
            
            data.setOnCemeteryFormed((center, size) -> {
                if (!isCemeteryAlreadyFormed(dimension, center)) {
                    markCemeteryAsFormed(dimension, center);

                    if (this.onCemeteryFormed != null) {
                        this.onCemeteryFormed.onFormed(dimension, center, size);
                    }
                }
            });
        }
    }


    @Setter
    private ICemeteryManager.LevelChecker levelChecker;

    private final Map<ResourceKey<Level>, Set<BlockPos>> formedCemeteries = new HashMap<>();

    private CemeteryManager() {
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
        formedCemeteries.remove(dimension);
    }

    public void clear() {
        dimensions.clear();
        formedCemeteries.clear();
    }

    private boolean isCemeteryAlreadyFormed(ResourceKey<Level> dimension, BlockPos center) {
        Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
        return formed.contains(center);
    }

    private void markCemeteryAsFormed(ResourceKey<Level> dimension, BlockPos center) {
        Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
        formed.add(center);
    }
    
    private void unmarkCemeteryAsFormed(ResourceKey<Level> dimension, BlockPos center) {
        Set<BlockPos> formed = formedCemeteries.get(dimension);
        if (formed != null) {
            formed.remove(center);
        }
    }


    public void validateAndCleanGraves() {
        for (Map.Entry<ResourceKey<Level>, DimensionGraveData> entry : dimensions.entrySet()) {
            ResourceKey<Level> dimension = entry.getKey();
            DimensionGraveData data = entry.getValue();

            Level level = levelChecker != null ? levelChecker.getLevel(dimension) : null;

            if (level != null && !level.isClientSide) {
                Set<BlockPos> allGraves = data.getAllGraves();
                Set<BlockPos> invalidGraves = new HashSet<>();

                for (BlockPos pos : allGraves) {
                    if (!isGraveBlockAtPosition(level, pos)) {
                        invalidGraves.add(pos);
                    }
                }

                for (BlockPos pos : invalidGraves) {
                    data.removeGrave(pos);
                }

                if (!invalidGraves.isEmpty()) {
                    if (level instanceof ServerLevel serverLevel) {
                        CemeterySavedData.markDirty(serverLevel);
                    }
                }
            }
        }
    }


    public void reevaluateCemeteries() {
        for (Map.Entry<ResourceKey<Level>, DimensionGraveData> entry : dimensions.entrySet()) {
            ResourceKey<Level> dimension = entry.getKey();
            DimensionGraveData data = entry.getValue();

            List<CemeteryInfo> cemeteries = data.getAllCemeteries();

            for (CemeteryInfo cemetery : cemeteries) {
                BlockPos center = cemetery.getCenter();
                int size = cemetery.getGraves().size();
                if (onCemeteryFormed != null) {
                    onCemeteryFormed.onFormed(dimension, center, size);
                }
            }
        }
    }


    private boolean isGraveBlockAtPosition(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        return block instanceof GraveStoneBlock;
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
        formedCemeteries.clear();

        for (String key : tag.getAllKeys()) {
            ResourceKey<Level> dimension = dimensionResolver.apply(key);
            if (dimension != null) {
                DimensionGraveData data = createDimensionData();
                data.load(tag.getCompound(key));
                dimensions.put(dimension, data);

                if (onCemeteryFormed != null) {
                    data.setOnCemeteryFormed((center, size) -> {
                        if (!isCemeteryAlreadyFormed(dimension, center)) {
                            markCemeteryAsFormed(dimension, center);
                            onCemeteryFormed.onFormed(dimension, center, size);
                        }
                    });
                }
                
                List<CemeteryInfo> cemeteries = data.getAllCemeteries();
                Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
                for (CemeteryInfo cemetery : cemeteries) {
                    formed.add(cemetery.getCenter());
                }
            }
        }
    }

    private DimensionGraveData getData(ResourceKey<Level> dimension) {
        return dimensions.computeIfAbsent(dimension, k -> {
            DimensionGraveData data = createDimensionData();

            data.setOnCemeteryFormed((center, size) -> {
                if (!isCemeteryAlreadyFormed(dimension, center)) {
                    markCemeteryAsFormed(dimension, center);

                    if (onCemeteryFormed != null) {
                        onCemeteryFormed.onFormed(dimension, center, size);
                    }
                }
            });
            
            data.setOnCemeteryDestroyed((oldRoot) -> {
                BlockPos center = data.getClusterCenter(oldRoot);
                unmarkCemeteryAsFormed(dimension, center);
            });
            return data;
        });
    }

    private DimensionGraveData createDimensionData() {
        return new DimensionGraveData(clusterRadius, minGravesForCemetery);
    }
}