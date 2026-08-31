package it.hurts.sskirillss.yagm.block;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.entity.GhostlyFogEntity;
import it.hurts.sskirillss.yagm.init.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import it.hurts.sskirillss.yagm.util.ParticleUtils;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.UUID;

public class GraveStoneBlock extends Block implements SimpleWaterloggedBlock, EntityBlock {

    private static final ResourceLocation TWILIGHT_PORTAL_ID = ResourceLocation.fromNamespaceAndPath("twilightforest", "twilight_portal");

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
            level.destroyBlock(gravePos, !serverPlayer.isCreative(), serverPlayer);
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
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity te, ItemStack stack) {
        if (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            Block.popResource(level, pos, new ItemStack(this));
            player.causeFoodExhaustion(0.005F);
        } else {
            super.playerDestroy(level, player, pos, state, te, stack);
        }
    }

    @Override
    public @NotNull BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos gravePos = (shape.isDouble() && state.getValue(HALF) == DoubleBlockHalf.UPPER) ? pos.below() : pos;
            GraveStoneBlockEntity grave = getBlockEntity(level, gravePos);

            if (grave != null && player instanceof ServerPlayer serverPlayer) {
                grave.giveInventoryToPlayer(serverPlayer);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
        return state;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != BlockEntityRegistry.GRAVE_STONE.get()) {
            return null;
        }

        if (level.isClientSide) {
            return (lvl, p, s, blockentity) -> clientTick(lvl, p, s, (GraveStoneBlockEntity) blockentity);
        }

        return (lvl, p, s, blockentity) -> serverTick((ServerLevel) lvl, p, s, (GraveStoneBlockEntity) blockentity);
    }

    private static void clientTick(Level level, BlockPos pos, BlockState state, GraveStoneBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            return;
        }

        if (blockEntity.getGraveLevel() != GraveStoneLevels.GRAVESTONE_LEVEL_3) {
            return;
        }


        IGraveVariant variant = blockEntity.getVariant();

        double[][] candles = variant != null ? variant.getCandlePositions() : null;

        if (candles == null) {
            return;
        }

        Direction facing = state.hasProperty(GraveStoneBlock.FACING) ? state.getValue(GraveStoneBlock.FACING) : Direction.NORTH;

        for (double[] candle : candles) {
            double lx = candle[0];
            double lz = candle[1];
            double lyOffset = candle[2];

            double ox, oz;
            switch (facing) {
                case SOUTH -> {
                    ox = -lx;
                    oz = -lz;
                }
                case EAST -> {
                    ox = -lz;
                    oz = lx;
                }
                case WEST -> {
                    ox = lz;
                    oz = -lx;
                }
                default -> {
                    ox = lx;
                    oz = lz;
                }
            }

            for (int i = 0; i < 3; i++) {
                double x = pos.getX() + 0.5 + ox + (level.random.nextDouble() - 0.5) * 0.03;
                double y = pos.getY() + lyOffset + level.random.nextDouble() * 0.04;
                double z = pos.getZ() + 0.5 + oz + (level.random.nextDouble() - 0.5) * 0.03;

                level.addParticle(ParticleUtils.constructSimpleSpark(new Color(155 + level.getRandom().nextInt(100), level.getRandom().nextInt(100), 0), 0.15f, 5 + level.getRandom().nextInt(5), 0.85f), x, y, z, 0.0, 0.025, 0.0);
            }
        }
    }

    private static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GraveStoneBlockEntity blockEntity) {
        if (blockEntity.isDecorative() || blockEntity.isVoidRecovery()) {
            return;
        }

        if (((level.getGameTime() + pos.asLong()) % 20L) != 0L) {
            return;
        }

        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0, false) == null) {
            return;
        }

        ResourceLocation variantId = blockEntity.getGraveData().getVariantId();
        if (variantId == null && blockEntity.getVariant() != null) {
            variantId = blockEntity.getVariant().getId();
        }

        CemeteryManager cemeteryManager = CemeteryManager.getInstance();
        if (!cemeteryManager.isCemetery(level.dimension(), pos)) {
            clearCemeteryFogForGrave(level, pos);
            return;
        }

        BlockPos lastAdded = cemeteryManager.getLastAddedCemeteryGrave(level.dimension());
        if (lastAdded != null && lastAdded.equals(pos)) {
            clearCemeteryFogForGrave(level, pos);
            return;
        }

        AABB checkBox = new AABB(pos).inflate(2.2);

        for (GhostlyFogEntity fogEntity : level.getEntitiesOfClass(GhostlyFogEntity.class, checkBox)) {
            if (fogEntity.isBoundToGrave(pos)) {
                return;
            }
        }

        float[] color = VariantUtils.getVariantColor(variantId != null ? variantId.getPath() : null);

        GhostlyFogEntity fog = EntityRegistry.GHOSTLY_FOG.get().create(level);

        if (fog == null) {
            return;
        }

        double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.12;
        double y = pos.getY() - 0.06;
        double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.12;

        fog.moveTo(x, y, z, 0f, 0f);
        fog.bindToGrave(pos);
        int density = 1;
        float scale = 1.01f;
        fog.configure(170 + level.random.nextInt(70), 1.42f + level.random.nextFloat() * 0.20f, density, -scale, color[0], color[1], color[2], 10);
        level.addFreshEntity(fog);
    }

    private static void clearCemeteryFogForGrave(ServerLevel level, BlockPos pos) {
        AABB checkBox = new AABB(pos).inflate(2.2);

        for (GhostlyFogEntity fogEntity : level.getEntitiesOfClass(GhostlyFogEntity.class, checkBox)) {
            if (fogEntity.isBoundToGrave(pos)) {
                fogEntity.discard();
            }
        }
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
        boolean handledTwilightRelocation = false;

        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (!shape.isDouble() || state.getValue(HALF) == DoubleBlockHalf.LOWER) {
                if (isTwilightPortal(newState) || isTwilightPortalCleanup(level, pos, newState)) {
                    handledTwilightRelocation = relocateTwilightPortalGrave(level, pos, state);
                }

                if (!handledTwilightRelocation && shape.isDouble()) {
                    BlockPos upperPos = pos.above();
                    if (level.getBlockState(upperPos).is(this)) {
                        level.removeBlock(upperPos, false);
                    }
                }
                if (!handledTwilightRelocation) {
                    GraveStoneBlockEntity be = getBlockEntity(level, pos);
                    if (be != null) {
                        be.dropItems(level, pos);
                    }
                    CemeteryManager.getInstance().removeGrave(level.dimension(), pos);
                }
            } else {
                BlockPos lowerPos = pos.below();
                if (level.getBlockState(lowerPos).is(this)) {
                    level.removeBlock(lowerPos, false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    private boolean relocateTwilightPortalGrave(Level level, BlockPos pos, BlockState state) {
        GraveStoneBlockEntity blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null || blockEntity.isDecorative()) {
            return false;
        }

        CompoundTag itemData = blockEntity.getItemData();
        GraveStoneLevels graveLevel = blockEntity.getGraveLevel();

        Direction facing = state.getValue(FACING);

        String variantId = itemData.contains(KEYS.getVariantId()) ? itemData.getString(KEYS.getVariantId()) : null;
        Block graveBlock = VariantUtils.getVariantId(variantId, graveLevel);
        BlockState graveState = graveBlock.defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, level.getFluidState(pos).isSourceOfType(Fluids.WATER));

        BlockPos targetPos = PlaceableUtils.findP2P(level, pos, 4);
        if (targetPos == null) {
            targetPos = PlaceableUtils.getGraveStoneBlockPosition(level, pos);
        }

        if (targetPos == null || targetPos.equals(pos) || !PlaceableUtils.placeGraveStone(level, targetPos, graveState)) {
            return false;
        }

        if (shape.isDouble()) {
            BlockPos upperPos = pos.above();
            if (level.getBlockState(upperPos).is(this)) {
                level.removeBlock(upperPos, false);
            }
        }

        if (level.getBlockEntity(targetPos) instanceof GraveStoneBlockEntity placedEntity) {
            placedEntity.loadGraveData(itemData, level.registryAccess());

            if (itemData.hasUUID(KEYS.getId()) && level instanceof ServerLevel serverLevel) {
                GraveDataManager.get(serverLevel).setGravePos(itemData.getUUID(KEYS.getId()), targetPos);
            }
        }

        CemeteryManager.getInstance().removeGrave(level.dimension(), pos);
        CemeteryManager.getInstance().addGrave(level.dimension(), targetPos);
        return true;
    }

    private static boolean isTwilightPortal(BlockState state) {
        return TWILIGHT_PORTAL_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    private static boolean isTwilightPortalCleanup(Level level, BlockPos pos, BlockState newState) {
        if (!newState.isAir()) {
            return false;
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -5; dy <= -1; dy++) {
                    if (isTwilightPortal(level.getBlockState(pos.offset(dx, dy, dz)))) {
                        return true;
                    }
                }
            }
        }

        return false;
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
        if (blockEntity != null && (blockEntity.isVoidRecovery() || blockEntity.isDecorative())) {
            return;
        }

        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            startFalling(level, pos, state);
        }
    }

    private void startFalling(ServerLevel level, BlockPos pos, BlockState state) {
        GraveStoneBlockEntity blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null) return;
        if (blockEntity.isDecorative()) return;

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

        FallingGraveEntity falling = FallingGraveEntity.create(level, new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D), Vec3.ZERO, itemData, graveLevel, ownerUUID, ownerName, state.getValue(FACING));
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
