package it.hurts.sskirillss.yagm.structure.cemetery.data;


import it.hurts.sskirillss.yagm.structure.cemetery.config.CemeteryConfig;
import it.hurts.sskirillss.yagm.structure.cemetery.util.SpatialGraveHash;
import it.hurts.sskirillss.yagm.structure.cemetery.util.UnionFind;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DimensionGraveData {

    @Getter
    private final int clusterRadius;

    @Getter
    private final int minGravesForCemetery;

    private final SpatialGraveHash spatialHash;
    private final UnionFind unionFind;
    private final Deque<BlockPos> insertionOrder = new ArrayDeque<>();

    private final Map<BlockPos, BlockPos> centerCache = new HashMap<>();
    private final Set<BlockPos> dirtyCenters = new HashSet<>();

    @Setter
    private BiConsumer<BlockPos, Integer> onCemeteryFormed;

    @Setter
    private Consumer<BlockPos> onCemeteryDestroyed;

    public DimensionGraveData(int clusterRadius, int minGravesForCemetery) {
        this.clusterRadius = clusterRadius;
        this.minGravesForCemetery = minGravesForCemetery;
        this.spatialHash = new SpatialGraveHash(CemeteryConfig.getCellSize());
        this.unionFind = new UnionFind();
    }


    public void addGrave(BlockPos pos) {
        if (!spatialHash.add(pos)) return;
        insertionOrder.remove(pos);
        insertionOrder.addLast(pos.immutable());

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
        insertionOrder.remove(pos);
        Set<BlockPos> oldClusterMembers = new HashSet<>(unionFind.getClusterMembers(pos));
        int oldClusterSize = oldClusterMembers.size();

        unionFind.remove(pos);
        oldClusterMembers.remove(pos);

        centerCache.clear();
        dirtyCenters.clear();

        if (!oldClusterMembers.isEmpty()) {
            rebuildCluster(oldClusterMembers);

            if (oldClusterSize >= minGravesForCemetery && onCemeteryDestroyed != null) {
                boolean anyCemeteryRemains = false;
                Set<BlockPos> checkedRoots = new HashSet<>();

                for (BlockPos member : oldClusterMembers) {
                    BlockPos root = unionFind.find(member);
                    if (root != null && checkedRoots.add(root)) {
                        if (unionFind.getClusterSize(member) >= minGravesForCemetery) {
                            anyCemeteryRemains = true;
                            break;
                        }
                    }
                }

                if (!anyCemeteryRemains) {
                    onCemeteryDestroyed.accept(pos);
                }
            }
        } else if (oldClusterSize >= minGravesForCemetery && onCemeteryDestroyed != null) {
            onCemeteryDestroyed.accept(pos);
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

    public BlockPos getLastAddedGrave() {
        return insertionOrder.peekLast();
    }

    public BlockPos getLastAddedCemeteryGrave() {
        Iterator<BlockPos> it = insertionOrder.descendingIterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (isCemetery(pos)) {
                return pos;
            }
        }
        return null;
    }


    public void clear() {
        spatialHash.clear();
        unionFind.clear();
        centerCache.clear();
        dirtyCenters.clear();
        insertionOrder.clear();
    }


    private void rebuildCluster(Set<BlockPos> graves) {
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
        ListTag orderList = new ListTag();

        for (BlockPos pos : spatialHash.getAll()) {
            BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(gravesList::add);
        }

        for (BlockPos pos : insertionOrder) {
            BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).result().ifPresent(orderList::add);
        }

        tag.put("graves", gravesList);
        tag.put("order", orderList);
        return tag;
    }

    public void load(CompoundTag tag) {
        clear();
        ListTag gravesList = tag.getList("graves", Tag.TAG_INT_ARRAY);
        for (Tag value : gravesList) {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, value).resultOrPartial(e -> {}).ifPresent(this::loadGravePos);
        }

        ListTag orderList = tag.getList("order", Tag.TAG_INT_ARRAY);
        for (Tag value : orderList) {
            BlockPos.CODEC.parse(NbtOps.INSTANCE, value).resultOrPartial(e -> {}).ifPresent(pos -> {
                if (spatialHash.getAll().contains(pos)) {
                    insertionOrder.remove(pos);
                    insertionOrder.addLast(pos.immutable());
                }
            });
        }

        if (insertionOrder.isEmpty()) {
            List<BlockPos> sorted = new ArrayList<>(spatialHash.getAll());
            sorted.sort(Comparator.comparingLong(BlockPos::asLong));
            for (BlockPos pos : sorted) {
                insertionOrder.addLast(pos.immutable());
            }
        }
    }

    private void loadGravePos(BlockPos pos) {
        if (!spatialHash.add(pos)) return;
        unionFind.makeSet(pos);
        for (BlockPos neighbor : spatialHash.findNeighborsInRadius(pos, clusterRadius)) {
            if (unionFind.union(pos, neighbor)) {
                invalidateCenter(unionFind.find(pos));
            }
        }
    }
}
