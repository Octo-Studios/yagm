package it.hurts.sskirillss.yagm.events;

import it.hurts.sskirillss.yagm.api.item_valuator.ItemValuator;
import it.hurts.sskirillss.yagm.blocks.gravestones.FallingGraveEntity;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.utils.ItemUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class GraveStoneEvent {

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        GraveStoneLevels graveLevel = calculateGraveLevel(player, graveData);

        Vec3 deathPos = player.position().add(0, 1, 0);

        double angle = level.random.nextDouble() * Math.PI * 2;
        double distance = 2.0 + level.random.nextDouble() * 2.0;
        double speed = 0.3 + level.random.nextDouble() * 0.2;

        Vec3 velocity = new Vec3(Math.cos(angle) * speed, 0.5 + level.random.nextDouble() * 0.3, Math.sin(angle) * speed);

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

    private static GraveStoneLevels calculateGraveLevel(ServerPlayer player, CompoundTag graveData) {
        if (!ItemValuator.isAvailable()) {
            return GraveStoneLevels.GRAVESTONE_LEVEL_1;
        }

        NonNullList<ItemStack> mainList = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armorList = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhandList = NonNullList.withSize(1, ItemStack.EMPTY);

        ItemUtils.readInventory(player.registryAccess(), graveData, "MainInventory", mainList);
        ItemUtils.readInventory(player.registryAccess(), graveData, "ArmorInventory", armorList);
        ItemUtils.readInventory(player.registryAccess(), graveData, "OffhandInventory", offhandList);

        double value = ItemValuator.getInstance().calculateValue(mainList, armorList, offhandList);
        GraveStoneLevels level = ItemValuator.getInstance().determineLevelByValue(value);

        return level;
    }

}