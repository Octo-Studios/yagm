package it.hurts.sskirillss.yagm.network.handler;

import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class GhostSpawnHandler {

    private record CemeteryKey(ResourceKey<Level> dimension, long centerPos) {}

    private static final Map<CemeteryKey, Integer> cooldowns = new HashMap<>();
    private static int tickCounter = 0;

    public static void tick(MinecraftServer server) {
        tickCounter++;

        if (!cooldowns.isEmpty()) {
            cooldowns.entrySet().removeIf(entry -> {
                entry.setValue(entry.getValue() - 1);
                return entry.getValue() <= 0;
            });
        }

        var checkInterval = Math.clamp(ConfigRegistry.CONFIG.getGhostSpawnCooldownSeconds() * 4, 1, GhostEntityData.CHECK_INTERVAL);

        if (tickCounter % checkInterval != 0 || !ConfigRegistry.CONFIG.isGhostSpawnEnabled()) {
            return;
        }

        CemeteryManager manager = CemeteryManager.getInstance();

        for (ResourceKey<Level> dimension : manager.getLoadedDimensions()) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null || level.isDay()) {
                continue;
            }

            List<CemeteryInfo> cemeteries = manager.getAllCemeteries(dimension);

            for (CemeteryInfo cemetery : cemeteries) {
                GhostSpawnHandler.spawnAtCemetery(level, cemetery);
            }
        }
    }

    private static void spawnAtCemetery(ServerLevel level, CemeteryInfo cemetery) {
        BlockPos center = cemetery.getCenter();
        CemeteryKey key = new CemeteryKey(level.dimension(), center.asLong());

        if (cooldowns.containsKey(key)) {
            return;
        }

        AABB searchBox = new AABB(center).inflate(GhostEntityData.GHOST_COUNT_SEARCH_RADIUS);

        int existingGhosts = level.getEntitiesOfClass(GhostEntity.class, searchBox, ghost -> !ghost.isTame()).size();

        if (existingGhosts >= ConfigRegistry.CONFIG.getMaxGhostsPerCemetery()) {
            return;
        }

        Set<BlockPos> graves = cemetery.getGraves();
        if (graves == null || graves.isEmpty()) {
            return;
        }

        List<BlockPos> spawnableGraves = new ArrayList<>();
        for (BlockPos gravePos : graves) {
            if (!level.isLoaded(gravePos)) {
                continue;
            }

            if (level.getBlockEntity(gravePos) instanceof GraveStoneBlockEntity blockEntity && !blockEntity.isDecorative()) {
                spawnableGraves.add(gravePos);
            }
        }

        if (spawnableGraves.isEmpty()) {
            return;
        }

        BlockPos spawnGrave = spawnableGraves.get(level.random.nextInt(spawnableGraves.size()));

        double spawnY = spawnGrave.getY() + 2.0 + level.random.nextDouble();
        BlockPos spawnPos = BlockPos.containing(spawnGrave.getX() + 0.5, spawnY, spawnGrave.getZ() + 0.5);

        GhostEntity ghost = new GhostEntity(EntityRegistry.GHOST.get(), level);
        ghost.moveTo(spawnPos.getX() + 0.5, spawnY, spawnPos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        ghost.setHomePos(spawnGrave);
        ghost.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);

        level.addFreshEntity(ghost);
        cooldowns.put(key, ConfigRegistry.CONFIG.getGhostSpawnCooldownSeconds() * 20);
    }

    public static void reset() {
        cooldowns.clear();
        tickCounter = 0;
    }
}
