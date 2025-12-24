package it.hurts.sskirillss.yagm.events;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.events.IServerEvent;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.utils.GraveStoneHelper;
import it.hurts.sskirillss.yagm.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.UUID;

public class GraveStoneEvent {

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos gravePos = GraveStoneHelper.getGraveStoneBlockPosition(level, player.blockPosition());
        BlockState graveState = BlockRegistry.GRAVE_STONE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection().getOpposite());

        serverLevel.getServer().execute(() -> placeGrave(serverLevel, gravePos, graveState, player, graveData));
    }

    public static void placeGrave(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, CompoundTag graveData) {
        if (!GraveStoneHelper.placeGraveStone(level, pos, state)) return;

        YAGMCommon.LOGGER.warn("[AsyncTask] Grave stone placed successfully at " + pos);

        GraveStoneBlockEntity graveEntity = new GraveStoneBlockEntity(pos, state);
        GraveDataManager manager = GraveDataManager.get(level);

        UUID graveId = graveData.contains("Id") ? graveData.getUUID("Id") : null;
        loadTransientInventory(graveEntity, manager, graveId, player, graveData);

        graveData.putUUID("PlayerUuid", player.getUUID());
        graveData.putString("PlayerName", player.getName().getString());
        graveData.putInt("XP", player.experienceLevel);

        graveEntity.setDeathTime(System.currentTimeMillis());
        graveEntity.loadGraveData(graveData);

        level.setBlockEntity(graveEntity);


        EventResult result = IServerEvent.ON_GRAVE_PLACED.invoker().onGravePlaced(level, pos, state, player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            level.removeBlock(pos, false);
            YAGMCommon.LOGGER.info("[AsyncTask] Grave placement interrupted by event for player: " + player.getUUID());
            return;
        }

        manager.addGrave(player.getUUID(), graveData);
        graveEntity.setChanged();

        YAGMCommon.LOGGER.info("[AsyncTask] Grave data loaded and saved for player: " + player.getUUID());
    }

    private static void loadTransientInventory(GraveStoneBlockEntity graveEntity, GraveDataManager manager, UUID graveId, ServerPlayer player, CompoundTag graveData) {
        NonNullList<ItemStack> mainList;
        NonNullList<ItemStack> armorList;
        NonNullList<ItemStack> offhandList;

        if (graveId != null && manager.getTransientMain(graveId) != null) {
            mainList = manager.getTransientMain(graveId);
            armorList = manager.getTransientArmor(graveId);
            offhandList = manager.getTransientOffhand(graveId);
        } else {
            mainList = NonNullList.withSize(36, ItemStack.EMPTY);
            armorList = NonNullList.withSize(4, ItemStack.EMPTY);
            offhandList = NonNullList.withSize(1, ItemStack.EMPTY);

            ItemUtils.readInventory(player.registryAccess(), graveData, "MainInventory", mainList);
            ItemUtils.readInventory(player.registryAccess(), graveData, "ArmorInventory", armorList);
            ItemUtils.readInventory(player.registryAccess(), graveData, "OffhandInventory", offhandList);
        }

        graveEntity.setTransientInventory(mainList, armorList, offhandList);
    }
}