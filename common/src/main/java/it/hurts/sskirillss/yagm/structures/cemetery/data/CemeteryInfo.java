package it.hurts.sskirillss.yagm.structures.cemetery.data;

import lombok.Data;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;


@Getter
@Data
public class CemeteryInfo {

    private final UUID id;
    private final Set<BlockPos> graves;
    private final BlockPos center;
    private final AABB bounds;
    private final int size;

    public CemeteryInfo(Set<BlockPos> graves) {
        this.id = UUID.randomUUID();
        this.graves = Collections.unmodifiableSet(graves);
        this.size = graves.size();
        this.center = calculateCenter(graves);
        this.bounds = calculateBounds(graves);
    }

    public boolean contains(BlockPos pos) {
        return graves.contains(pos);
    }

    public boolean isWithinBounds(BlockPos pos) {
        return bounds.contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public double distanceTo(BlockPos pos) {
        return Math.sqrt(center.distSqr(pos));
    }

    public double getDiameter() {
        return Math.sqrt(Math.pow(bounds.maxX - bounds.minX, 2) + Math.pow(bounds.maxZ - bounds.minZ, 2));
    }

    static BlockPos calculateCenter(Set<BlockPos> graves) {
        if (graves.isEmpty()) return BlockPos.ZERO;

        long sumX = 0, sumY = 0, sumZ = 0;
        for (BlockPos pos : graves) {
            sumX += pos.getX();
            sumY += pos.getY();
            sumZ += pos.getZ();
        }

        int count = graves.size();
        return new BlockPos(
                (int) (sumX / count),
                (int) (sumY / count),
                (int) (sumZ / count)
        );
    }

    private static AABB calculateBounds(Set<BlockPos> graves) {
        if (graves.isEmpty()) {
            return new AABB(0, 0, 0, 0, 0, 0);
        }

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : graves) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CemeteryInfo object = (CemeteryInfo) o;
        return id.equals(object.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}