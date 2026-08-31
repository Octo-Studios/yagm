package it.hurts.sskirillss.yagm.structure.cemetery;

import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.structure.cemetery.config.CemeteryConfig;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeteryInfo;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import it.hurts.sskirillss.yagm.structure.cemetery.data.DimensionGraveData;
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

    @Setter
    private ICemeteryManager.LevelChecker levelChecker;

    private final Map<ResourceKey<Level>, Set<BlockPos>> formedCemeteries = new HashMap<>();
    private final Map<ResourceKey<Level>, BlockPos> lastAddedGraves = new HashMap<>();


    public void addGrave(ResourceKey<Level> dimension, BlockPos pos) {
        Level level = levelChecker != null ? levelChecker.getLevel(dimension) : null;
        if (level != null && !isCemeteryGravePos(level, pos)) {
            return;
        }

        DimensionGraveData data = getData(dimension);
        data.addGrave(pos);
        lastAddedGraves.put(dimension, pos.immutable());
    }

    private void setupCallbacks(ResourceKey<Level> dimension, DimensionGraveData data) {
        data.setOnCemeteryFormed((center, size) -> {
            if (!isCemeteryAlreadyFormed(dimension, center)) {
                markCemeteryAsFormed(dimension, center);
                if (onCemeteryFormed != null) {
                    onCemeteryFormed.onFormed(dimension, center, size);
                }
            }
        });
        data.setOnCemeteryDestroyed((removedPos) -> refreshFormedCemeteries(dimension, data));
    }

    public void removeGrave(ResourceKey<Level> dimension, BlockPos pos) {
        DimensionGraveData data = getData(dimension);
        data.removeGrave(pos);

        BlockPos last = lastAddedGraves.get(dimension);
        if (last != null && last.equals(pos)) {
            BlockPos nextLast = data.getLastAddedGrave();
            if (nextLast == null) {
                lastAddedGraves.remove(dimension);
            } else {
                lastAddedGraves.put(dimension, nextLast.immutable());
            }
        }
    }


    public boolean isCemetery(ResourceKey<Level> dimension, BlockPos pos) {
        return getData(dimension).isCemetery(pos);
    }


    public Set<BlockPos> getGravesInRadius(ResourceKey<Level> dimension, BlockPos pos, int radius) {
        return getData(dimension).getGravesInRadius(pos, radius);
    }


    public int getGraveCountNear(ResourceKey<Level> dimension, BlockPos pos, int radius) {
        return getData(dimension).getGraveCountNear(pos, radius);
    }


    public BlockPos getLastAddedCemeteryGrave(ResourceKey<Level> dimension) {
        DimensionGraveData data = getData(dimension);
        BlockPos last = lastAddedGraves.get(dimension);

        if (last != null && data.isCemetery(last)) {
            return last;
        }

        BlockPos fallback = data.getLastAddedCemeteryGrave();
        if (fallback != null) {
            lastAddedGraves.put(dimension, fallback.immutable());
        }
        return fallback;
    }


    public List<CemeteryInfo> getAllCemeteries(ResourceKey<Level> dimension) {
        return getData(dimension).getAllCemeteries();
    }

    private boolean isCemeteryAlreadyFormed(ResourceKey<Level> dimension, BlockPos center) {
        Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
        for (BlockPos existing : formed) {
            if (existing.distSqr(center) < 25) {
                return true;
            }
        }
        return false;
    }

    private void markCemeteryAsFormed(ResourceKey<Level> dimension, BlockPos center) {
        Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
        formed.add(center);
    }


    private void refreshFormedCemeteries(ResourceKey<Level> dimension, DimensionGraveData data) {
        Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
        formed.clear();
        for (CemeteryInfo cemetery : data.getAllCemeteries()) {
            formed.add(cemetery.getCenter());
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
                    if (!isCemeteryGravePos(level, pos)) {
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

    private boolean isCemeteryGravePos(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return true;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (!(block instanceof GraveStoneBlock)) {
            return false;
        }

        if (level.getBlockEntity(pos) instanceof GraveStoneBlockEntity blockEntity) {
            return !blockEntity.isDecorative();
        }

        return true;
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
        lastAddedGraves.clear();

        for (String key : tag.getAllKeys()) {
            ResourceKey<Level> dimension = dimensionResolver.apply(key);
            if (dimension != null) {
                DimensionGraveData data = createDimensionData();
                data.load(tag.getCompound(key));
                dimensions.put(dimension, data);

                setupCallbacks(dimension, data);

                List<CemeteryInfo> cemeteries = data.getAllCemeteries();
                Set<BlockPos> formed = formedCemeteries.computeIfAbsent(dimension, k -> new HashSet<>());
                for (CemeteryInfo cemetery : cemeteries) {
                    formed.add(cemetery.getCenter());
                }

                BlockPos last = data.getLastAddedGrave();

                if (last != null) {
                    lastAddedGraves.put(dimension, last.immutable());
                }
            }
        }
    }

    private DimensionGraveData getData(ResourceKey<Level> dimension) {
        return dimensions.computeIfAbsent(dimension, k -> {
            DimensionGraveData data = createDimensionData();
            setupCallbacks(dimension, data);
            return data;
        });
    }

    private DimensionGraveData createDimensionData() {
        return new DimensionGraveData(clusterRadius, minGravesForCemetery);
    }

    public void resetRuntimeState() {
        dimensions.clear();
        formedCemeteries.clear();
        lastAddedGraves.clear();
        onCemeteryFormed = null;
        levelChecker = null;
    }
}
