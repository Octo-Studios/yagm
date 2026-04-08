package it.hurts.sskirillss.yagm.block;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class GraveStoneBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private final GraveStoneShape shape;

    public GraveStoneBlock(Properties properties, GraveStoneShape shape) {
        super(properties);
        this.shape = shape;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, false));
    }

    public boolean isDoubleShape() {
        return shape.isDouble();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.getBlockState(pos.below()).is(Blocks.WATER)) {
            return null;
        }

        boolean waterlogged = level.getFluidState(pos).isSourceOfType(Fluids.WATER);
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER).setValue(WATERLOGGED, waterlogged);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && shape.isDouble()) {
            BlockPos upperPos = pos.above();
            BlockState upperCurrent = level.getBlockState(upperPos);
            if (upperCurrent.isAir() || upperCurrent.is(Blocks.WATER)) {
                boolean upperWaterlogged = level.getFluidState(upperPos).isSourceOfType(Fluids.WATER);
                level.setBlock(upperPos, state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(WATERLOGGED, upperWaterlogged), 3);
            }
        }

        if (!level.isClientSide()) {
            GraveStoneBlockEntity be = getBlockEntity(level, pos);
            if (be != null) {
                CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                if (!customData.isEmpty()) {
                    be.loadFromBlockItemData(customData.copyTag(), level.registryAccess());
                }

                if (be.isDecorative()) {
                    CemeteryManager.getInstance().removeGrave(level.dimension(), pos);
                } else {
                    CemeteryManager.getInstance().addGrave(level.dimension(), pos);
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockPos gravePos = pos;

        if (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            gravePos = pos.below();
        }

        GraveStoneBlockEntity be = getBlockEntity(level, gravePos);
        if (be == null || be.isDecorative() || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            be.giveInventoryToPlayer(serverPlayer);
            level.removeBlock(gravePos, false);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        boolean ew = facing == Direction.EAST || facing == Direction.WEST;

        if (shape.isDouble()) {
            if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
                if (ew) {
                    return shape.getUpperEastWest();
                } else {
                    return shape.getUpperNorthSouth();
                }
            }
        }

        if (ew) {
            return shape.getEastWest();
        } else {
            return shape.getNorthSouth();
        }
    }

    @Override
    public @NotNull VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }


    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos gravePos = (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) ? pos.below() : pos;
            GraveStoneBlockEntity grave = getBlockEntity(level, gravePos);

            if (grave != null && player instanceof ServerPlayer serverPlayer) {
                grave.giveInventoryToPlayer(serverPlayer);
                ItemStack graveItem = new ItemStack(level.getBlockState(gravePos).getBlock());
                ItemEntity ie = new ItemEntity(level, gravePos.getX() + 0.5, gravePos.getY() + 0.5, gravePos.getZ() + 0.5, graveItem);
                ie.setDefaultPickUpDelay();
                level.addFreshEntity(ie);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }

        return super.getFluidState(state);
    }

    @Override
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (!shape.isDouble() || state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (shape.isDouble()) {
                    BlockPos upperPos = pos.above();
                    if (level.getBlockState(upperPos).is(this)) {
                        level.removeBlock(upperPos, false);
                    }
                }
                GraveStoneBlockEntity be = getBlockEntity(level, pos);
                if (be != null) {
                    be.dropItems(level, pos);
                }
                CemeteryManager.getInstance().removeGrave(level.dimension(), pos);
            } else {
                BlockPos lowerPos = pos.below();
                if (level.getBlockState(lowerPos).is(this)) {
                    level.removeBlock(lowerPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide() && !isMoving && oldState.isAir()) {
            if (!shape.isDouble() || state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                GraveStoneBlockEntity be = getBlockEntity(level, pos);
                if (be != null && !be.isDecorative()) {
                    CemeteryManager.getInstance().addGrave(level.dimension(), pos);
                }
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, WATERLOGGED);
    }


    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (level.isClientSide()) return;

        BlockPos lowerPos;

        if (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            lowerPos = pos.below();
        } else {
            lowerPos = pos;
        }

        level.scheduleTick(lowerPos, this, 2);
    }


    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) return;

        GraveStoneBlockEntity blockEntity = getBlockEntity(level, pos);
        if (blockEntity != null && blockEntity.isVoidRecovery()) {
            return;
        }

        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            startFalling(level, pos, state);
        }
    }

    private void startFalling(ServerLevel level, BlockPos pos, BlockState state) {
        GraveStoneBlockEntity blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null) return;

        boolean hasLandingSurface = false;
        for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            if (!FallingBlock.isFree(level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())))) {
                hasLandingSurface = true;
                break;
            }
        }
        if (!hasLandingSurface) {
            blockEntity.setSuppressDropsOnRemove(false);
            level.removeBlock(pos, false);
            return;
        }

        GraveStoneLevels graveLevel = blockEntity.getGraveLevel();
        CompoundTag itemData = blockEntity.getItemData();

        if (!itemData.contains(KEYS.getVariantId())) {
            IGraveVariant variant = blockEntity.getVariant();
            if (variant != null && variant.getId() != null) {
                itemData.putString(KEYS.getVariantId(), variant.getId().toString());
            }
        }

        UUID ownerUUID = blockEntity.getGraveData().getOwnerUUID();
        String ownerName = blockEntity.getGraveData().getOwnerName();

        blockEntity.setSuppressDropsOnRemove(true);
        level.removeBlock(pos, false);

        FallingGraveEntity falling = FallingGraveEntity.create(level, Vec3.atCenterOf(pos), Vec3.ZERO, itemData, graveLevel, ownerUUID, ownerName, state.getValue(FACING));
        falling.stopRotation();
        level.addFreshEntity(falling);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) return null;
        return new GraveStoneBlockEntity(pos, state);
    }

    @Nullable
    private static GraveStoneBlockEntity getBlockEntity(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof GraveStoneBlockEntity graveStoneBlockEntity) {
            return graveStoneBlockEntity;
        }
        return null;
    }
}
