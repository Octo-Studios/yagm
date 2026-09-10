package it.hurts.sskirillss.yagm.component.placement;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.ContainerUtils;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class GravePlacementService {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;


    public static boolean placeImmediate(ServerLevel level, BlockPos pos, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, Direction facing, boolean strictPlacement, boolean recovery) {
        ResourceLocation variantId = getVariantId(level, pos, graveData);

        BlockState graveState = createState(VariantUtils.getVariantId(variantId != null ? variantId.toString() : null, graveLevel), level, pos, facing);

        BlockPos placedPos = null;

        if (strictPlacement) {
            if (PlaceableUtils.placeGraveStoneExact(level, pos, graveState)) {
                placedPos = pos.immutable();
            }
        } else if (PlaceableUtils.placeGraveStoneExact(level, pos, graveState)) {
            placedPos = pos.immutable();
        } else {
            placedPos = PlaceableUtils.placeGraveStoneAndGetPos(level, pos, graveState);
        }

        if (placedPos == null) {
            if (strictPlacement) {
                dropUnplacedGrave(level, pos, graveData);
                return false;
            }

            BlockPos supportTopPos = new BlockPos(pos.getX(), level.getMinBuildHeight() + 1, pos.getZ());

            if (!supportTopPos.equals( pos) && PlaceableUtils.placeGraveStoneExact(level, supportTopPos, graveState)) {
                placedPos = supportTopPos.immutable();
            } else if (!supportTopPos.equals(pos)) {
                placedPos = PlaceableUtils.placeGraveStoneAndGetPos(level, supportTopPos, graveState);
            }

            if (placedPos == null) {
                dropUnplacedGrave(level, pos, graveData);
                return false;
            }
        }

        finishPlacement(level, placedPos, graveData, graveLevel, ownerUUID, ownerName, variantId, recovery, false);

        return true;
    }

    public static GravePlacementResult placeFalling(ServerLevel level, BlockPos pos, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, Direction facing, @Nullable ResourceLocation variantId, boolean voidRecovery) {
        if (isSameGraveAlreadyPlaced(level, pos, graveData)) {
            return GravePlacementResult.placed(pos, level.getBlockState(pos));
        }

        BlockState graveState = createState(VariantUtils.getVariantId(variantId != null ? variantId.toString() : null, graveLevel), level, pos, facing);
        BlockPos placedPos;

        if (voidRecovery) {
            placedPos = PlaceableUtils.placeGraveStoneExact(level, pos, graveState) ? pos.immutable() : null;
        } else {
            placedPos = PlaceableUtils.placeGraveStoneAndGetPos(level, pos, graveState);
        }

        if (placedPos == null) {
            return GravePlacementResult.failed();
        }

        finishPlacement(level, placedPos, graveData, graveLevel, ownerUUID, ownerName, variantId, voidRecovery, true);

        return GravePlacementResult.placed(placedPos, graveState);
    }

    public static void dropUnplacedGrave(ServerLevel level, BlockPos pos, CompoundTag graveData) {
        ContainerUtils.dropFullGrave(level, pos, graveData);

        if (graveData.hasUUID(KEYS.getId())) {
            GraveDataManager.get(level).removeGrave(graveData.getUUID(KEYS.getId()));
        }
    }

    @Nullable
    private static ResourceLocation getVariantId(ServerLevel level, BlockPos pos, CompoundTag graveData) {
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

        return variantId;
    }

    private static BlockState createState(Block graveBlock, ServerLevel level, BlockPos pos, Direction facing) {
        return graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER));
    }

    private static void finishPlacement(ServerLevel level, BlockPos pos, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, @Nullable ResourceLocation variantId, boolean recovery, boolean addCemetery) {
        if (level.getBlockEntity(pos) instanceof GraveStoneBlockEntity blockEntity) {
            blockEntity.loadGraveData(graveData, level.registryAccess());
            blockEntity.setVoidRecovery(recovery);
            blockEntity.initializeGrave(ownerUUID, ownerName, System.currentTimeMillis(), null, null, graveLevel);

            if (variantId != null) {
                blockEntity.setVariant(GraveVariantRegistry.get(variantId));
            }

            if (addCemetery && !blockEntity.isDecorative()) {
                CemeteryManager.getInstance().addGrave(level.dimension(), pos);
            }
        }

        if (graveData.hasUUID(KEYS.getId())) {
            GraveDataManager.get(level).setGravePos(graveData.getUUID(KEYS.getId()), pos);
        }
    }

    private static boolean isSameGraveAlreadyPlaced(ServerLevel level, BlockPos pos, CompoundTag graveData) {
        return graveData != null && graveData.hasUUID(KEYS.getId()) && level.getBlockEntity(pos) instanceof GraveStoneBlockEntity existing
                && graveData.getUUID(KEYS.getId()).equals(existing.getGraveData().getGraveId());
    }
}
