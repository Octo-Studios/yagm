package it.hurts.sskirillss.yagm.network.handler;

import it.hurts.sskirillss.yagm.entity.GhostEntity;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeteryInfo;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.*;


@Slf4j
public class GhostSpawnHandler {

    private static final int SPAWN_COOLDOWN_TICKS = 1200;
    private static final int MAX_GHOSTS_PER_CEMETERY = 5;
    private static final int MIN_GHOSTS_PER_CEMETERY = 1;
    private static final int CHECK_INTERVAL = 100;
    private static final double GHOST_COUNT_SEARCH_RADIUS = 64.0;

    private static final Map<Long, Integer> cooldowns = new HashMap<>();
    private static int tickCounter = 0;

    public static void tick(MinecraftServer server) {
        tickCounter++;

        if (!cooldowns.isEmpty()) {
            cooldowns.entrySet().removeIf(e -> {
                e.setValue(e.getValue() - 1);
                return e.getValue() <= 0;
            });
        }


        if (tickCounter % CHECK_INTERVAL != 0) return;

        CemeteryManager manager = CemeteryManager.getInstance();

        for (ResourceKey<Level> dimKey : manager.getLoadedDimensions()) {
            ServerLevel level = null;
            for (ServerLevel sl : server.getAllLevels()) {
                if (sl.dimension().equals(dimKey)) {
                    level = sl;
                    break;
                }
            }
            if (level == null) continue;


            if (level.isDay()) continue;

            List<CemeteryInfo> cemeteries = manager.getAllCemeteries(dimKey);
            for (CemeteryInfo cemetery : cemeteries) {
                spawnAtCemetery(level, manager, dimKey, cemetery);
            }
        }
    }

    private static void spawnAtCemetery(ServerLevel level, CemeteryManager manager, ResourceKey<Level> dimKey, CemeteryInfo cemetery) {
        BlockPos center = cemetery.getCenter();
        long centerKey = center.asLong();

        if (cooldowns.containsKey(centerKey)) return;


        AABB searchBox = new AABB(center).inflate(GHOST_COUNT_SEARCH_RADIUS);
        int existingGhosts = level.getEntitiesOfClass(GhostEntity.class, searchBox, ghost -> !ghost.isTame()).size();

        if (existingGhosts >= MAX_GHOSTS_PER_CEMETERY) return;

        Set<BlockPos> graves = cemetery.getGraves();
        if (graves == null || graves.isEmpty()) return;

        List<BlockPos> graveList = new ArrayList<>(graves);
        BlockPos spawnGrave = graveList.get(level.random.nextInt(graveList.size()));

        if (!level.isLoaded(spawnGrave)) return;

        double spawnY = spawnGrave.getY() + 2.0 + level.random.nextDouble();
        BlockPos spawnPos = BlockPos.containing(spawnGrave.getX() + 0.5, spawnY, spawnGrave.getZ() + 0.5);

        GhostEntity ghost = new GhostEntity(EntityRegistry.GHOST.get(), level);
        ghost.moveTo(spawnPos.getX() + 0.5, spawnY, spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        ghost.setHomePos(spawnGrave);
        ghost.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);

        level.addFreshEntity(ghost);


        cooldowns.put(centerKey, SPAWN_COOLDOWN_TICKS);
    }


    public static void reset() {
        cooldowns.clear();
        tickCounter = 0;
    }
}
