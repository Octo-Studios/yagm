package it.hurts.sskirillss.yagm.structures.cemetery.data;


import it.hurts.sskirillss.yagm.structures.cemetery.config.CemeteryConfig;
import it.hurts.sskirillss.yagm.structures.cemetery.utils.SpatialHash;
import it.hurts.sskirillss.yagm.structures.cemetery.utils.clustering.UnionFind;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Data on graves in ONLY one dimension.
 */
public class DimensionGraveData {

    @Getter
    private final int clusterRadius;

    @Getter
    private final int minGravesForCemetery;

    private final SpatialHash spatialHash;
    private final UnionFind unionFind;

    private final Map<BlockPos, BlockPos> centerCache = new HashMap<>();
    private final Set<BlockPos> dirtyCenters = new HashSet<>();

    @Setter
    private BiConsumer<BlockPos, Integer> onCemeteryFormed;
    
    @Setter
    private Consumer<BlockPos> onCemeteryDestroyed;

    public DimensionGraveData(int clusterRadius, int minGravesForCemetery) {
        this.clusterRadius = clusterRadius;
        this.minGravesForCemetery = minGravesForCemetery;
        this.spatialHash = new SpatialHash(CemeteryConfig.getCellSize());
        this.unionFind = new UnionFind();
    }


    public void addGrave(BlockPos pos) {
        if (!spatialHash.add(pos)) return;

        unionFind.makeSet(pos);

        Set<BlockPos> neighbors = spatialHash.findNeighborsInRadius(pos, clusterRadius);
        for (BlockPos neighbor : neighbors) {
            if (unionFind.union(pos, neighbor)) {
                invalidateCenter(unionFind.find(pos));
            }
        }

        int clusterSize = getClusterSize(pos);
        
        if (clusterSize >= minGravesForCemetery && onCemeteryFormed != null) {
            onCemeteryFormed.accept(getClusterCenter(pos), clusterSize);
        }
    }

    public void removeGrave(BlockPos pos) {
        if (!spatialHash.remove(pos)) return;

        BlockPos oldRoot = unionFind.find(pos);
        int oldClusterSize = unionFind.getClusterSize(pos);
        
        Set<BlockPos> neighbors = spatialHash.findInRadius(pos, clusterRadius);

        unionFind.remove(pos);
        invalidateCenter(oldRoot);

        if (!neighbors.isEmpty()) {
            rebuildToCluster(neighbors);

            if (oldClusterSize >= minGravesForCemetery && onCemeteryDestroyed != null) {
                int newClusterSize = getClusterSize(pos);
                if (newClusterSize < minGravesForCemetery) {
                    onCemeteryDestroyed.accept(oldRoot);
                }
            }
        }
    }


    public boolean isCemetery(BlockPos pos) {
        return getClusterSize(pos) >= minGravesForCemetery;
    }


    public int getClusterSize(BlockPos pos) {
        return unionFind.getClusterSize(pos);
    }


    public Set<BlockPos> getClusterGraves(BlockPos pos) {
        return unionFind.getClusterMembers(pos);
    }


    public BlockPos getClusterCenter(BlockPos pos) {
        BlockPos root = unionFind.find(pos);
        if (root == null) return pos;

        if (dirtyCenters.contains(root) || !centerCache.containsKey(root)) {
            BlockPos center = CemeteryInfo.calculateCenter(getClusterGraves(pos));
            centerCache.put(root, center);
            dirtyCenters.remove(root);
        }

        return centerCache.get(root);
    }


    public Set<BlockPos> getGravesInRadius(BlockPos center, int radius) {
        return spatialHash.findInRadius(center, radius);
    }


    public int getGraveCountNear(BlockPos center, int radius) {
        return spatialHash.findInRadius(center, radius).size();
    }

    /**
     * Get All Cemeteries
     */
    public List<CemeteryInfo> getAllCemeteries() {
        List<CemeteryInfo> result = new ArrayList<>();

        Map<BlockPos, Set<BlockPos>> clusters = unionFind.getAllClusters();
        for (Set<BlockPos> cluster : clusters.values()) {
            if (cluster.size() >= minGravesForCemetery) {
                result.add(new CemeteryInfo(cluster));
            }
        }

        return result;
    }


    public Set<BlockPos> getAllGraves() {
        return spatialHash.getAll();
    }


    public int getGraveCount() {
        return spatialHash.getTotalCount();
    }


    public boolean containsGrave(BlockPos pos) {
        return spatialHash.contains(pos);
    }

    public void clear() {
        spatialHash.clear();
        unionFind.clear();
        centerCache.clear();
        dirtyCenters.clear();
    }

    public boolean isEmpty() {
        return spatialHash.isEmpty();
    }


    private void rebuildToCluster(Set<BlockPos> graves) {
        unionFind.resetElements(graves);
        for (BlockPos grave : graves) {
            centerCache.remove(grave);
        }
        List<BlockPos> graveList = new ArrayList<>(graves);
        long radiusSq = (long) clusterRadius * clusterRadius;

        for (int i = 0; i < graveList.size(); i++) {
            for (int j = i + 1; j < graveList.size(); j++) {
                BlockPos a = graveList.get(i);
                BlockPos b = graveList.get(j);

                if (distanceSq2D(a, b) <= radiusSq) {
                    unionFind.union(a, b);
                }
            }
        }
    }

    private void invalidateCenter(BlockPos root) {
        if (root != null) {
            dirtyCenters.add(root);
        }
    }

    private long distanceSq2D(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag gravesList = new ListTag();

        for (BlockPos pos : spatialHash.getAll()) {
            CompoundTag graveTag = new CompoundTag();
            graveTag.putInt("x", pos.getX());
            graveTag.putInt("y", pos.getY());
            graveTag.putInt("z", pos.getZ());
            gravesList.add(graveTag);
        }

        tag.put("graves", gravesList);
        return tag;
    }

    public void load(CompoundTag tag) {
        clear();
        ListTag gravesList = tag.getList("graves", Tag.TAG_COMPOUND);
        for (int i = 0; i < gravesList.size(); i++) {
            CompoundTag graveTag = gravesList.getCompound(i);
            BlockPos pos = new BlockPos(
                    graveTag.getInt("x"),
                    graveTag.getInt("y"),
                    graveTag.getInt("z")
            );

            if (!spatialHash.add(pos)) continue;
            unionFind.makeSet(pos);
            
            Set<BlockPos> neighbors = spatialHash.findNeighborsInRadius(pos, clusterRadius);
            for (BlockPos neighbor : neighbors) {
                if (unionFind.union(pos, neighbor)) {
                    invalidateCenter(unionFind.find(pos));
                }
            }
        }
    }
}