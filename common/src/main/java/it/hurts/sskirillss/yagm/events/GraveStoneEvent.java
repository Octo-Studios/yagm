package it.hurts.sskirillss.yagm.events;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.events.providers.IServerEvent;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.FallingGraveEntity;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.network.handlers.InventoryHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GraveStoneEvent {

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        GraveStoneLevels graveLevel = InventoryHelper.calculateGraveLevel(player, graveData);

        Vec3 deathPos = player.position().add(0, 0.5, 0);

        double angle = level.random.nextDouble() * Math.TAU;
        double speed = 0.4 + level.random.nextDouble() * 0.2;

        Vec3 velocity = new Vec3(Math.cos(angle) * speed, 0.60 + level.random.nextDouble() * 0.1, Math.sin(angle) * speed);

        Direction facing = player.getDirection().getOpposite();

        UUID graveId = graveData.hasUUID("Id") ? graveData.getUUID("Id") : UUID.randomUUID();
        graveData.putUUID("Id", graveId);

        GraveDataManager manager = GraveDataManager.get(serverLevel);
        manager.addGrave(player.getUUID(), graveData);

        serverLevel.getServer().execute(() -> {
            FallingGraveEntity fallingGrave = FallingGraveEntity.create(serverLevel, deathPos, velocity, graveData, graveLevel, player.getUUID(), player.getName().getString(), facing);
            serverLevel.addFreshEntity(fallingGrave);
        });

    }

    public static void handlePlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        CompoundTag graveData = InventoryHelper.savePlayerInventory(player);

        EventResult result = IServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            YAGMCommon.LOGGER.info("Player death event interrupted for: " + player.getUUID());
            return;
        }

        if (AccessoryManager.hasAnyHandler()) {
            AccessoryManager.clearAllAccessories(player);
        }

        player.getInventory().clearContent();
    }
}