package it.hurts.sskirillss.yagm.event.death.service;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.placement.GravePlacementPlan;
import it.hurts.sskirillss.yagm.component.placement.GravePositionResolver;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.data.gravedata.GraveSaveManager;
import it.hurts.sskirillss.yagm.event.death.service.helper.Placer;
import it.hurts.sskirillss.yagm.event.death.tracker.GraveDeathTracker;
import it.hurts.sskirillss.yagm.util.*;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;

import java.util.Collection;
import java.util.UUID;

@Slf4j
public final class GraveDeathService {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    public static boolean handle(ServerPlayer player, Collection<ItemEntity> drops) {
        if (player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return false;
        }

        if (!GraveDeathTracker.begin(player)) {
            return false;
        }

        try {
            CompoundTag graveData = InventoryUtils.savePlayerInventory(player);

            ContainerUtils.saveDroppedItems(graveData, drops, player.registryAccess());

            if (!hasRecoverableData(player, graveData)) {
                return false;
            }

            EventResult result = IServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

            if (result.interruptsFurtherEvaluation() || graveData.getBoolean(KEYS.interrupted())) {
                return false;
            }

            GraveDeathTracker.set(player);
            clearRecoveredData(player);

            return true;
        } catch (Exception exception) {
            log.error("Failed to handle death grave for {}", player.getGameProfile().getName(), exception);
            return false;
        } finally {
            GraveDeathTracker.end(player);
        }
    }

    private static boolean hasRecoverableData(ServerPlayer player, CompoundTag graveData) {
        boolean isInventory = !NBTReaderUtils.getAllItemsFromNBT(player.registryAccess(), graveData).isEmpty();

        boolean extradata = graveData.contains(KEYS.getAccessories()) || graveData.contains(KEYS.getBackpacks()) || graveData.contains(KEYS.getDroppedItems());

        return isInventory || extradata || player.totalExperience > 0;
    }

    private static void clearRecoveredData(ServerPlayer player) {
        AccessoryLoader.clearAccessories(player);
        BackpackLoader.clearBackpacks(player);
        player.getInventory().clearContent();
        TierUtils.clearExperience(player);
    }

    public static void place(ServerPlayer player, CompoundTag graveData) {

        if (!(player.level() instanceof ServerLevel level)) {
            suppressgrave(graveData);
            return;
        }

        UUID graveId = getrootId(graveData);

        save(level, player, graveId, graveData);

        GraveStoneLevels graveLevel = GraveLevelUtils.calculateGraveLevel(player);

        GravePlacementPlan plan = GravePositionResolver.resolveDeath(player, GraveDeathTracker.trackedSpawnPos(level, player), GraveDeathTracker.trackedPlacementPos(level, player));

        boolean placed = Placer.place(level, player, graveData, graveLevel, plan);

        if (!placed) {
            return;
        }
    }

    private static UUID getrootId(CompoundTag data){
        if (!data.hasUUID(KEYS.getId())) {
            data.putUUID(KEYS.getId(), UUID.randomUUID());
        }

        return data.getUUID(KEYS.getId());
    }

    private static void suppressgrave(CompoundTag graveData) {
        graveData.putBoolean(KEYS.interrupted(), true);
    }

    private static void save(ServerLevel level, ServerPlayer player, UUID id, CompoundTag graveData) {
        GraveDataManager manager = GraveDataManager.get(level);

        if (!graveData.hasUUID(KEYS.getId())) {
            return;
        }

        if (!manager.hasGrave(id)) {
            manager.addGrave(graveData);
        }

        GraveSaveManager.savedata(level, player.getUUID(), System.currentTimeMillis(), graveData);
    }
}
