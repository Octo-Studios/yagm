package it.hurts.sskirillss.yagm.structures.cemetery.utils;

import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.*;


public class SpatialHash {

    private final int cellSize;
    private final Map<Long, Set<BlockPos>> cells = new HashMap<>();

    @Getter
    private int totalCount = 0;

    public SpatialHash(int cellSize) {
        this.cellSize = cellSize;
    }

    public boolean add(BlockPos pos) {
        long cell = toCell(pos);
        Set<BlockPos> cellSet = cells.computeIfAbsent(cell, k -> new HashSet<>());

        if (cellSet.add(pos)) {
            totalCount++;
            return true;
        }
        return false;
    }

    public boolean remove(BlockPos pos) {
        long cell = toCell(pos);
        Set<BlockPos> cellSet = cells.get(cell);

        if (cellSet != null && cellSet.remove(pos)) {
            totalCount--;
            if (cellSet.isEmpty()) {
                cells.remove(cell);
            }
            return true;
        }
        return false;
    }

    public boolean contains(BlockPos pos) {
        long cell = toCell(pos);
        Set<BlockPos> cellSet = cells.get(cell);
        return cellSet != null && cellSet.contains(pos);
    }


    public Set<BlockPos> findInRadius(BlockPos center, int radius) {
        Set<BlockPos> result = new HashSet<>();

        int cellRadius = (radius / cellSize) + 1;
        int centerCellX = center.getX() / cellSize;
        int centerCellZ = center.getZ() / cellSize;

        long radiusSq = (long) radius * radius;

        for (int dx = -cellRadius; dx <= cellRadius; dx++) {
            for (int dz = -cellRadius; dz <= cellRadius; dz++) {
                long cell = toCellKey(centerCellX + dx, centerCellZ + dz);
                Set<BlockPos> cellSet = cells.get(cell);

                if (cellSet != null) {
                    for (BlockPos pos : cellSet) {
                        if (distanceSq2D(center, pos) <= radiusSq) {
                            result.add(pos);
                        }
                    }
                }
            }
        }

        return result;
    }


    public Set<BlockPos> findNeighborsInRadius(BlockPos center, int radius) {
        Set<BlockPos> result = findInRadius(center, radius);
        result.remove(center);
        return result;
    }


    public Set<BlockPos> getAll() {
        Set<BlockPos> result = new HashSet<>();
        for (Set<BlockPos> cellSet : cells.values()) {
            result.addAll(cellSet);
        }
        return result;
    }

    public void clear() {
        cells.clear();
        totalCount = 0;
    }

    public boolean isEmpty() {
        return totalCount == 0;
    }

    private long toCell(BlockPos pos) {
        return toCellKey(pos.getX() / cellSize, pos.getZ() / cellSize);
    }

    private long toCellKey(int cellX, int cellZ) {
        return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
    }

    private long distanceSq2D(BlockPos a, BlockPos b) {
        long dx = a.getX() - b.getX();
        long dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }
}