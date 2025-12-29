package it.hurts.sskirillss.yagm.events;

import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.events.providers.IServerEvent;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.titles.entity.GraveTitleEntity;
import it.hurts.sskirillss.yagm.client.titles.render.component.GravestoneTitle;
import it.hurts.sskirillss.yagm.client.titles.renderer.GravestoneTitles;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import it.hurts.sskirillss.yagm.utils.GraveStoneHelper;
import it.hurts.sskirillss.yagm.utils.ItemUtils;
import it.hurts.sskirillss.yagm.utils.ItemValuator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.UUID;

public class GraveStoneEvent {

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        Level level = player.level();
        if (level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        GraveStoneLevels graveLevel = calculateGraveLevel(player, graveData);
        BlockPos gravePos = GraveStoneHelper.getGraveStoneBlockPosition(level, player.blockPosition());

        Block graveBlock = BlockRegistry.getBlockForLevel(graveLevel);

        BlockState graveState = graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, player.getDirection().getOpposite());

        serverLevel.getServer().execute(() -> placeGrave(serverLevel, gravePos, graveState, player, graveData, graveLevel));
    }

    private static GraveStoneLevels calculateGraveLevel(ServerPlayer player, CompoundTag graveData) {
        if (!ItemValuator.isAvailable()) {
            return GraveStoneLevels.GRAVESTONE_LEVEL_2;
        }

        NonNullList<ItemStack> mainList = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armorList = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhandList = NonNullList.withSize(1, ItemStack.EMPTY);

        ItemUtils.readInventory(player.registryAccess(), graveData, "MainInventory", mainList);
        ItemUtils.readInventory(player.registryAccess(), graveData, "ArmorInventory", armorList);
        ItemUtils.readInventory(player.registryAccess(), graveData, "OffhandInventory", offhandList);

        double value = ItemValuator.getInstance().calculateListsValue(mainList, armorList, offhandList);
        GraveStoneLevels level = ItemValuator.getInstance().determineLevelByValue(value);

        return level;
    }

    public static void placeGrave(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, CompoundTag graveData, GraveStoneLevels graveLevel) {
        if (!GraveStoneHelper.placeGraveStone(level, pos, state)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof GraveStoneBlockEntity graveEntity)) return;


        GraveDataManager manager = GraveDataManager.get(level);
        UUID graveId = graveData.contains("Id") ? graveData.getUUID("Id") : null;

        NonNullList<ItemStack> mainList = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armorList = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhandList = NonNullList.withSize(1, ItemStack.EMPTY);

        if (graveId != null && manager.getTransientMain(graveId) != null) {
            copyList(manager.getTransientMain(graveId), mainList);
            copyList(manager.getTransientArmor(graveId), armorList);
            copyList(manager.getTransientOffhand(graveId), offhandList);
        } else {
            ItemUtils.readInventory(player.registryAccess(), graveData, "MainInventory", mainList);
            ItemUtils.readInventory(player.registryAccess(), graveData, "ArmorInventory", armorList);
            ItemUtils.readInventory(player.registryAccess(), graveData, "OffhandInventory", offhandList);
        }

        graveEntity.setTransientInventory(mainList, armorList, offhandList);

        graveData.putUUID("PlayerUuid", player.getUUID());
        graveData.putString("PlayerName", player.getName().getString());
        graveData.putInt("XP", player.experienceLevel);

        Component deathCause = graveData.contains("DeathCause") ? Component.literal(graveData.getString("DeathCause")) : null;

        graveEntity.loadGraveData(graveData);

        graveEntity.initializeGrave(player.getUUID(), player.getName().getString(), System.currentTimeMillis(), deathCause, null, graveLevel);

        EventResult result = IServerEvent.ON_GRAVE_PLACED.invoker().onGravePlaced(level, pos, state, player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            level.removeBlock(pos, false);
            return;
        }

        manager.addGrave(player.getUUID(), graveData);
        graveEntity.setChanged();

        spawnGraveTitles(level, pos, graveEntity);
    }

    private static void spawnGraveTitles(ServerLevel level, BlockPos pos, GraveStoneBlockEntity graveEntity) {
        GravestoneTitles titles = graveEntity.getGravestoneTitles();
        if (titles == null) return;

        GravestoneTitle[] visibleTitles = titles.getVisibleTitles();

        float yOffset = 1.3f;
        for (GravestoneTitle title : visibleTitles) {
            GraveTitleEntity titleEntity = new GraveTitleEntity(((GraveTitleEntity) EntityRegistry.GRAVE_TITLE).getType(), level);

            titleEntity.setPos(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5);
            titleEntity.setText(title.getText());
            titleEntity.setColor(title.getColor());
            titleEntity.setLinkedGravePos(pos);

            level.addFreshEntity(titleEntity);

            yOffset += 0.25f;
        }
    }



    private static void copyList(NonNullList<ItemStack> source, NonNullList<ItemStack> target) {
        if (source == null) return;
        for (int i = 0; i < Math.min(source.size(), target.size()); i++) {
            target.set(i, source.get(i).copy());
        }
    }
}