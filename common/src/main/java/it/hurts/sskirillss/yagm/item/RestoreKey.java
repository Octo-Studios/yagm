package it.hurts.sskirillss.yagm.item;

import dev.architectury.networking.NetworkManager;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.data.gravedata.GraveSaveManager;
import it.hurts.sskirillss.yagm.network.packet.RestoreKeyActivationPacket;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class RestoreKey extends Item {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final String TAG_RESTORE_READY = "YAGMRestoreReady";

    public RestoreKey(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (level.isClientSide() || !(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }

        boolean available = getLastRestorableSave(serverPlayer) != null && !serverPlayer.getCooldowns().isOnCooldown(this);
        boolean current = isRestoreReady(stack);

        if (current == available) {
            return;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (available) {
                tag.putBoolean(TAG_RESTORE_READY, true);
            } else {
                tag.remove(TAG_RESTORE_READY);
            }
        });
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isRestoreReady(stack) || super.isFoil(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (!isRestoreReady(itemStack)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (level.isClientSide()) {
            return InteractionResultHolder.success(itemStack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(itemStack);
        }

        if (getLastRestorableSave(serverPlayer) == null) {
            return InteractionResultHolder.fail(itemStack);
        }

        player.startUsingItem(usedHand);

        return InteractionResultHolder.consume(itemStack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 30;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide) {
            return stack;
        }

        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return stack;
        }

        CompoundTag restorableSave = getLastRestorableSave(serverPlayer);
        if (restorableSave == null) {
            return stack;
        }

        CompoundTag lastSave = GraveSaveManager.consumeLatestRestoreSave(serverPlayer.serverLevel(), serverPlayer.getUUID());

        if (lastSave == null) {
            return stack;
        }

        if (restorableSave.hasUUID(KEYS.getId()) && lastSave.hasUUID(KEYS.getId()) && !restorableSave.getUUID(KEYS.getId()).equals(lastSave.getUUID(KEYS.getId()))) {
            return stack;
        }

        InventoryUtils.restoreFullGrave(serverPlayer, lastSave);
        cleanupSourceGrave(serverPlayer, lastSave);

        serverPlayer.getCooldowns().addCooldown(this, 60);


        NetworkManager.sendToPlayer(serverPlayer, new RestoreKeyActivationPacket(stack.copy()));

        stack.shrink(1);

        return stack;
    }

    private static void cleanupSourceGrave(ServerPlayer serverPlayer, CompoundTag saveData) {
        if (!saveData.hasUUID(KEYS.getId())) {
            return;
        }

        UUID graveId = saveData.getUUID(KEYS.getId());
        ServerLevel graveLevel = resolveGraveLevel(serverPlayer, saveData);

        GraveDataManager manager = GraveDataManager.get(graveLevel);
        manager.markRestoreKeyConsumed(graveId);

        BlockPos gravePos = manager.getGravePos(graveId);
        if (gravePos == null) {
            gravePos = findByDeathArea(graveLevel, saveData, graveId);
        }

        if (gravePos == null) {
            manager.removeGrave(graveId);
            return;
        }

        graveLevel.getChunk(gravePos.getX() >> 4, gravePos.getZ() >> 4);

        if (graveLevel.getBlockEntity(gravePos) instanceof GraveStoneBlockEntity blockEntity && graveId.equals(blockEntity.getGraveData().getGraveId())) {
            blockEntity.consumeByRestoreKey();

            graveLevel.destroyBlock(gravePos, false, serverPlayer);
        } else {
            manager.removeGrave(graveId);
        }
    }

    private static ServerLevel resolveGraveLevel(ServerPlayer serverPlayer, CompoundTag saveData) {
        if (saveData.contains(KEYS.getDimension())) {
            ResourceLocation dimId = ResourceLocation.tryParse(saveData.getString(KEYS.getDimension()));

            if (dimId != null) {
                ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
                ServerLevel resolved = serverPlayer.getServer().getLevel(dimKey);

                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return serverPlayer.serverLevel();
    }

    private static BlockPos findByDeathArea(ServerLevel level, CompoundTag saveData, UUID graveId) {
        if (!saveData.contains(KEYS.getDeathPosX()) || !saveData.contains(KEYS.getDeathPosY()) || !saveData.contains(KEYS.getDeathPosZ())) {
            return null;
        }

        BlockPos center = BlockPos.containing(saveData.getDouble(KEYS.getDeathPosX()), saveData.getDouble(KEYS.getDeathPosY()), saveData.getDouble(KEYS.getDeathPosZ()));

        for (BlockPos pos : CemeteryManager.getInstance().getGravesInRadius(level.dimension(), center, 64)) {
            level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

            if (level.getBlockEntity(pos) instanceof GraveStoneBlockEntity blockEntity && graveId.equals(blockEntity.getGraveData().getGraveId())) {
                return pos;
            }
        }

        return null;
    }

    private static boolean isRestoreReady(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(TAG_RESTORE_READY)) {
            return false;
        }
        return customData.copyTag().getBoolean(TAG_RESTORE_READY);
    }

    private static CompoundTag getLastRestorableSave(ServerPlayer serverPlayer) {
        CompoundTag save = GraveSaveManager.peekLatestRestoreSave(serverPlayer.serverLevel(), serverPlayer.getUUID());
        if (save == null || !save.hasUUID(KEYS.getId())) {
            return null;
        }

        UUID graveId = save.getUUID(KEYS.getId());
        ServerLevel saveLevel = resolveGraveLevel(serverPlayer, save);
        GraveDataManager manager = GraveDataManager.get(saveLevel);
        if (!manager.hasGrave(graveId)) {
            return null;
        }

        return save;
    }
}
