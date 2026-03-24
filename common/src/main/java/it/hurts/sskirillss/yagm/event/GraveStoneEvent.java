package it.hurts.sskirillss.yagm.event;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import lombok.extern.slf4j.Slf4j;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.component.type.GraveStoneLevels;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@Slf4j
public class GraveStoneEvent {

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        GraveStoneLevels graveLevel = InventoryUtils.calculateGraveLevel(player, graveData);

        Vec3 deathPos = player.position().add(0, 0.5, 0);

        Vec3 velocity = FallingGraveMotionConfig.DEFAULT.randomLaunchVelocity(level.random);

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
        CompoundTag graveData = InventoryUtils.savePlayerInventory(player);

        EventResult result = IServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            log.info("Player death event interrupted for: {}", player.getUUID());
            return;
        }

        if (AccessoryManager.hasAnyHandler()) {
            AccessoryManager.clearAllAccessories(player);
        }

        player.getInventory().clearContent();
    }
}