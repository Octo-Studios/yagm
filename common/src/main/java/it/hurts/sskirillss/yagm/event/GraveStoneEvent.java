package it.hurts.sskirillss.yagm.event;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.placement.DeathPlacementMode;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.data.gravedata.GraveSaveManager;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class GraveStoneEvent {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final Set<UUID> activeDeaths = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, TrackedSafePosition> lastSafePositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastHandledDeathTick = new ConcurrentHashMap<>();

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
        UUID graveId = graveData.getUUID(KEYS.getId());

        GraveDataManager manager = GraveDataManager.get(serverLevel);
        if (manager.hasGrave(graveId)) {
            return;
        }
        manager.addGrave(graveData);

        long deathTime = System.currentTimeMillis();
        GraveSaveManager.saveGraveData(serverLevel, player.getUUID(), deathTime, graveData);

        GraveStoneLevels graveLevel = InventoryUtils.calculateGraveLevel(player);

        DeathPlacementMode placementMode = resolvePlacementMode(player);

        if (placementMode == DeathPlacementMode.FALLING) {
            serverLevel.getServer().execute(() -> {
                FallingGraveEntity fallingGrave = FallingGraveEntity.create(serverLevel, deathPos, velocity, graveData, graveLevel, player.getUUID(), player.getName().getString(), facing, spawn.usedTrackedPosition());
                serverLevel.addFreshEntity(fallingGrave);
            });
        } else {
            BlockPos immediatePos;
            if (placementMode == DeathPlacementMode.UNDER_BEDROCK) {
                immediatePos = PlaceableUtils.getBedrockPlacement(serverLevel, player.blockPosition());
            } else {
                immediatePos = PlaceableUtils.getVoidRecovery(serverLevel, player, trackedPlacementPos(serverLevel, player));
            }

            ensureVoidRecoverySupport(serverLevel, immediatePos);
            placeImmediateGrave(serverLevel, immediatePos, graveData, graveLevel, player, facing);
        }
    }


    public static void trackLastSafePositions(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());
            updateTrackedSafePosition(player);
        }

        lastSafePositions.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
        lastHandledDeathTick.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    public static void resetRuntimeState() {
        activeDeaths.clear();
        lastSafePositions.clear();
        lastHandledDeathTick.clear();
    }

    public static void handlePlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        UUID uuid = player.getUUID();
        long currentTick = player.serverLevel().getGameTime();
        Long previousTick = lastHandledDeathTick.put(uuid, currentTick);
        if (previousTick != null && currentTick - previousTick <= 2L) {
            return;
        }

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

    private static DeathPlacementMode resolvePlacementMode(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();

        if (pos.getY() < level.getMinBuildHeight()) {
            return DeathPlacementMode.UNDER_BEDROCK;
        }

        if (isVoidDeath(player)) {
            return DeathPlacementMode.VOID;
        }

        return DeathPlacementMode.FALLING;
    }

    private static BlockPos trackedPlacementPos(ServerLevel level, ServerPlayer player) {
        TrackedSafePosition tracked = lastSafePositions.get(player.getUUID());
        if (tracked != null && tracked.dimension().equals(level.dimension())) {
            return BlockPos.containing(tracked.position());
        }

        return null;
    }

    private static void placeImmediateGrave(ServerLevel level, BlockPos pos, CompoundTag graveData, GraveStoneLevels graveLevel, ServerPlayer player, Direction facing) {
        ResourceLocation variantId = null;
        if (graveData.contains(KEYS.getVariantId())) {
            variantId = ResourceLocation.tryParse(graveData.getString(KEYS.getVariantId()));
        }

        if (variantId == null) {
            IGraveVariant variant = GraveVariantRegistry.getFor(level, pos);
            if (variant != null && variant.getId() != null) {
                variantId = variant.getId();
                graveData.putString(KEYS.getVariantId(), variantId.toString());
            }
        }

        Block graveBlock = VariantUtils.getVariantId(variantId != null ? variantId.toString() : null, graveLevel);
        BlockState graveState = graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER));

        if (!PlaceableUtils.placeGraveStoneExact(level, pos, graveState) && !PlaceableUtils.placeGraveStone(level, pos, graveState)) {
            InventoryUtils.dropFullGrave(level, pos, graveData);
            return;
        }

        BlockPos placedPos = findPlacedImmediateGravePos(level, pos, graveData);
        if (level.getBlockEntity(placedPos) instanceof GraveStoneBlockEntity blockEntity) {
            blockEntity.loadGraveData(graveData, level.registryAccess());

            blockEntity.setVoidRecovery(true);

            blockEntity.initializeGrave(player.getUUID(), player.getName().getString(), System.currentTimeMillis(), null, null, graveLevel);

            if (variantId != null) {
                blockEntity.setVariant(GraveVariantRegistry.get(variantId));
            }
        }

        if (graveData.hasUUID(KEYS.getId())) {
            GraveDataManager.get(level).setGravePos(graveData.getUUID(KEYS.getId()), placedPos);
        }
    }

    private static void ensureVoidRecoverySupport(ServerLevel level, BlockPos pos) {
        BlockPos supportPos = new BlockPos(pos.getX(), level.getMinBuildHeight(), pos.getZ());
        BlockState supportState = level.getBlockState(supportPos);
        if (supportState.isAir() || supportState.canBeReplaced()) {
            level.setBlock(supportPos, PlaceableUtils.getBlockForLevel(level), 3);
        }
    }

    private static BlockPos findPlacedImmediateGravePos(ServerLevel level, BlockPos origin, CompoundTag graveData) {
        UUID graveId = graveData.hasUUID(KEYS.getId()) ? graveData.getUUID(KEYS.getId()) : null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof GraveStoneBlockEntity blockEntity) {
                        if (graveId == null || graveId.equals(blockEntity.getGraveData().getGraveId())) {
                            return cursor.immutable();
                        }
                    }
                }
            }
        }

        return origin.immutable();
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
