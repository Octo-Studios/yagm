package it.hurts.sskirillss.yagm.structures.cemetery.utils.clustering;

import net.minecraft.core.BlockPos;

import java.util.*;


public class UnionFind {

    private final Map<BlockPos, BlockPos> parent = new HashMap<>();
    private final Map<BlockPos, Integer> rank = new HashMap<>();
    private final Map<BlockPos, Integer> size = new HashMap<>();


    public boolean makeSet(BlockPos pos) {
        if (parent.containsKey(pos)) {
            return false;
        }

        parent.put(pos, pos);
        rank.put(pos, 0);
        size.put(pos, 1);
        return true;
    }

    public void remove(BlockPos pos) {
        parent.remove(pos);
        rank.remove(pos);
        size.remove(pos);
    }

    public boolean contains(BlockPos pos) {
        return parent.containsKey(pos);
    }


    public BlockPos find(BlockPos pos) {
        BlockPos p = parent.get(pos);
        if (p == null) return null;

        if (!p.equals(pos)) {
            BlockPos root = find(p);
            parent.put(pos, root);
            return root;
        }
        return pos;
    }


    public boolean union(BlockPos a, BlockPos b) {
        BlockPos rootA = find(a);
        BlockPos rootB = find(b);

        if (rootA == null || rootB == null || rootA.equals(rootB)) {
            return false;
        }

        int rankA = rank.getOrDefault(rootA, 0);
        int rankB = rank.getOrDefault(rootB, 0);

        BlockPos newRoot, child;
        if (rankA < rankB) {
            newRoot = rootB;
            child = rootA;
        } else if (rankA > rankB) {
            newRoot = rootA;
            child = rootB;
        } else {
            newRoot = rootA;
            child = rootB;
            rank.put(rootA, rankA + 1);
        }

        parent.put(child, newRoot);

        int sizeA = size.getOrDefault(rootA, 1);
        int sizeB = size.getOrDefault(rootB, 1);
        size.put(newRoot, sizeA + sizeB);
        size.remove(child);

        return true;
    }


    public boolean connected(BlockPos a, BlockPos b) {
        BlockPos rootA = find(a);
        BlockPos rootB = find(b);
        return rootA != null && rootA.equals(rootB);
    }


    public int getClusterSize(BlockPos pos) {
        BlockPos root = find(pos);
        if (root == null) return 0;
        return size.getOrDefault(root, 1);
    }


    public Set<BlockPos> getClusterMembers(BlockPos pos) {
        BlockPos root = find(pos);
        if (root == null) return Collections.emptySet();

        Set<BlockPos> result = new HashSet<>();
        for (BlockPos member : parent.keySet()) {
            if (root.equals(find(member))) {
                result.add(member);
            }
        }
        return result;
    }


    public Set<BlockPos> getAllRoots() {
        Set<BlockPos> roots = new HashSet<>();
        for (BlockPos pos : parent.keySet()) {
            roots.add(find(pos));
        }
        return roots;
    }


    public Map<BlockPos, Set<BlockPos>> getAllClusters() {
        Map<BlockPos, Set<BlockPos>> clusters = new HashMap<>();

        for (BlockPos pos : parent.keySet()) {
            BlockPos root = find(pos);
            clusters.computeIfAbsent(root, k -> new HashSet<>()).add(pos);
        }

        return clusters;
    }


    public Set<BlockPos> getAllElements() {
        return Collections.unmodifiableSet(parent.keySet());
    }


    public void resetElements(Collection<BlockPos> elements) {
        for (BlockPos pos : elements) {
            if (parent.containsKey(pos)) {
                parent.put(pos, pos);
                rank.put(pos, 0);
                size.put(pos, 1);
            }
        }
    }

    public void clear() {
        parent.clear();
        rank.clear();
        size.clear();
    }

    public int getTotalCount() {
        return parent.size();
    }

    public boolean isEmpty() {
        return parent.isEmpty();
    }
}