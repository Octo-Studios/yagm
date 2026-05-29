package it.hurts.sskirillss.yagm.event;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import lombok.extern.slf4j.Slf4j;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.data.gravedata.GraveSaveManager;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class GraveStoneEvent {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final Set<UUID> activeDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, TrackedSafePosition> lastSafePositions = new ConcurrentHashMap<>();

    private record TrackedSafePosition(ResourceKey<Level> dimension, Vec3 position) {}

    private record DeathSpawn(Vec3 position, boolean usedTrackedPosition) {}

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();

        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        DeathSpawn spawn = resolveDeathSpawn(player);
        Vec3 deathPos = spawn.position();
        Vec3 velocity = spawn.usedTrackedPosition() ? Vec3.ZERO : FallingGraveMotionConfig.DEFAULT.randomLaunchVelocity(level.random);

        Direction facing = player.getDirection().getOpposite();

        if (!graveData.hasUUID(KEYS.getId())) graveData.putUUID(KEYS.getId(), UUID.randomUUID());

        GraveDataManager manager = GraveDataManager.get(serverLevel);
        manager.addGrave(graveData);

        long deathTime = System.currentTimeMillis();
        GraveSaveManager.saveGraveData(serverLevel, player.getUUID(), deathTime, graveData);

        GraveStoneLevels graveLevel = InventoryUtils.calculateGraveLevel(player);

        serverLevel.getServer().execute(() -> {
            FallingGraveEntity fallingGrave = FallingGraveEntity.create(serverLevel, deathPos, velocity, graveData, graveLevel, player.getUUID(), player.getName().getString(), facing, spawn.usedTrackedPosition());
            serverLevel.addFreshEntity(fallingGrave);
        });
    }

    public static void trackLastSafePositions(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());
            updateTrackedSafePosition(player);
        }

        lastSafePositions.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    public static void resetRuntimeState() {
        activeDeaths.clear();
        lastSafePositions.clear();
    }

    public static void handlePlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        UUID uuid = player.getUUID();
        if (!activeDeaths.add(uuid)) {
            return;
        }

        try {
            CompoundTag graveData = InventoryUtils.savePlayerInventory(player);

            boolean hasItems = !InventoryUtils.getAllItemsFromNBT(player.registryAccess(), graveData).isEmpty();
            boolean hasAccessories = graveData.contains(KEYS.getAccessories());
            if (!hasItems && !(player.experienceLevel > 0 || player.experienceProgress > 0) && !hasAccessories) {
                return;
            }

            EventResult result = IServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

            if (result.interruptsFurtherEvaluation()) {
                log.info("Player death event interrupted for: {}", uuid);
                return;
            }

            if (AccessoryLoader.hasAnyHandler()) {
                try {
                    AccessoryLoader.clearAccessories(player);
                } catch (Exception e) {
                    log.error("Failed to clear accessories for player {}: {}", uuid, e.getMessage());
                }
            }

            player.getInventory().clearContent();

            InventoryUtils.clearExperience(player);
        } catch (Exception e) {
            log.error("Failed to create grave for player {}, items will drop normally: {}", uuid, e.getMessage(), e);
        } finally {
            activeDeaths.remove(uuid);
        }
    }

    private static void updateTrackedSafePosition(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }

        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (pos.getY() <= level.getMinBuildHeight() + 2) {
            return;
        }

        if (isVoidColumn(level, pos)) {
            return;
        }

        lastSafePositions.put(player.getUUID(), new TrackedSafePosition(level.dimension(), player.position().add(0, 0.5, 0)));
    }

    private static DeathSpawn resolveDeathSpawn(ServerPlayer player) {
        Vec3 currentDeathPos = player.position().add(0, 0.5, 0);

        if (!isVoidDeath(player)) {
            return new DeathSpawn(currentDeathPos, false);
        }

        TrackedSafePosition tracked = lastSafePositions.get(player.getUUID());
        if (tracked == null || !tracked.dimension().equals(player.level().dimension())) {
            return new DeathSpawn(currentDeathPos, true);
        }

        return new DeathSpawn(tracked.position(), true);
    }

    private static boolean isVoidDeath(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (pos.getY() <= level.getMinBuildHeight() + 2) {
            return true;
        }

        return isVoidColumn(level, pos);
    }

    private static boolean isVoidColumn(Level level, BlockPos origin) {
        int minY = level.getMinBuildHeight();
        int startY = Math.min(origin.getY(), level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), startY, origin.getZ());

        for (int y = startY; y >= minY; y--) {
            cursor.setY(y);
            if (!level.getBlockState(cursor).isAir() || !level.getFluidState(cursor).isEmpty()) {
                return false;
            }
        }

        return true;
    }
}
