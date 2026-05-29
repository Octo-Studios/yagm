package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class FallingGraveEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROTATION = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_VARIANT = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_FACING = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROT_SPEED = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.FLOAT);

    private CompoundTag graveData;
    private UUID ownerUUID;
    private String ownerName;
    private boolean voidRecovery = false;
    private GraveStoneLevels graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;
    private Direction facing = Direction.NORTH;
    @Nullable
    private ResourceLocation variantId;

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final FallingGraveMotionConfig MOTION = FallingGraveMotionConfig.DEFAULT;

    private float rotationSpeed;
    private float prevRotation;
    private int lifetime = 0;
    private boolean graveHandled = false;
    private BlockPos lastSafePos = null;
    private static final TicketType<Long> FALLING_GRAVE_TICKET = TicketType.create("yagm_falling_grave", Long::compareTo);
    private final Set<Long> activeChunkTickets = new HashSet<>();

    public FallingGraveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static FallingGraveEntity create(Level level, Vec3 position, Vec3 velocity, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, Direction facing) {
        return create(level, position, velocity, graveData, graveLevel, ownerUUID, ownerName, facing, false);
    }

    public static FallingGraveEntity create(Level level, Vec3 position, Vec3 velocity, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, Direction facing, boolean voidRecovery) {
        FallingGraveEntity entity = new FallingGraveEntity(EntityRegistry.FALLING_GRAVE.get(), level);
        entity.setPos(position);
        entity.setDeltaMovement(velocity);
        entity.graveData = graveData;
        entity.graveLevel = graveLevel;
        entity.ownerUUID = ownerUUID;
        entity.ownerName = ownerName;
        entity.voidRecovery = voidRecovery;
        entity.facing = facing;
        if (voidRecovery) {
            entity.lastSafePos = BlockPos.containing(position);
        }
        entity.rotationSpeed = MOTION.randomRotSpeed(level.random);

        BlockPos blockPos = BlockPos.containing(position);

        if (graveData.contains(KEYS.getVariantId())) {
            ResourceLocation existingVariant = ResourceLocation.tryParse(graveData.getString(KEYS.getVariantId()));
            if (existingVariant != null) {
                entity.variantId = existingVariant;
                entity.entityData.set(DATA_VARIANT, existingVariant.toString());
            }
        } else {
            IGraveVariant variant = GraveVariantRegistry.getFor(level, blockPos);
            if (variant != null) {
                entity.variantId = variant.getId();
                graveData.putString(KEYS.getVariantId(), variant.getId().toString());
                entity.entityData.set(DATA_VARIANT, variant.getId().toString());
            }
        }

        entity.entityData.set(DATA_LEVEL, graveLevel.ordinal());
        entity.entityData.set(DATA_ROTATION, 0f);
        entity.entityData.set(DATA_FACING, facing.get2DDataValue());
        entity.entityData.set(DATA_ROT_SPEED, entity.rotationSpeed);

        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_LEVEL, 0);
        builder.define(DATA_ROTATION, 0f);
        builder.define(DATA_VARIANT, "");
        builder.define(DATA_FACING, Direction.NORTH.get2DDataValue());
        builder.define(DATA_ROT_SPEED, 20f);
    }

    @Override
    public void tick() {
        super.tick();
        lifetime++;

        if (!level().isClientSide()) {
            updateChunkTickets();
        }

        prevRotation = entityData.get(DATA_ROTATION);
        if (!onGround()) {
            entityData.set(DATA_ROTATION, (prevRotation + entityData.get(DATA_ROT_SPEED)) % 360f);
        }

        if (!level().isClientSide()) {
            if (shouldDiscardAfterRestoreKeyUse()) {
                graveHandled = true;
                discard();
                return;
            }

            if (!voidRecovery && getY() > level().getMinBuildHeight() + 8) {
                lastSafePos = blockPosition();
            }

            if (this.getY() < level().getMinBuildHeight() + 2) {
                placeGrave();
                return;
            }

            Vec3 motion = getDeltaMovement();
            setDeltaMovement(MOTION.applyPhysics(motion));
            move(MoverType.SELF, getDeltaMovement());

            boolean shouldPlace = onGround();

            if (!shouldPlace && motion.y < 0) {
                BlockPos belowPos = blockPosition().below();
                BlockState belowState = level().getBlockState(belowPos);

                if (belowState.isSuffocating(level(), belowPos) || belowState.isSolid()) {
                    BlockPos pos = blockPosition();
                    BlockState state = level().getBlockState(pos);

                    if (!state.isAir() && !state.canBeReplaced()) {
                        shouldPlace = true;
                    }
                }
            }

            if (!shouldPlace && lifetime > 300) {
                shouldPlace = true;
            }

            if (shouldPlace) {
                placeGrave();
            }
        } else {
            if (!onGround()) {
                setDeltaMovement(MOTION.applyPhysics(getDeltaMovement()));
                move(MoverType.SELF, getDeltaMovement());
            }
        }
    }

    private boolean shouldDiscardAfterRestoreKeyUse() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (graveData == null || !graveData.hasUUID(KEYS.getId())) {
            return false;
        }

        UUID graveId = graveData.getUUID(KEYS.getId());
        return GraveDataManager.get(serverLevel).isRestoreKeyConsumed(graveId);
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide() && !graveHandled && reason == RemovalReason.KILLED) {
            graveHandled = true;
            handleGravePlacement();
        }
        if (!level().isClientSide()) {
            if (!(level() instanceof ServerLevel serverLevel) || activeChunkTickets.isEmpty()) {
                return;
            }

            for (Long chunkId : activeChunkTickets) {
                ChunkPos chunk = new ChunkPos(chunkId);
                serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
            }

            activeChunkTickets.clear();
        }
        super.remove(reason);
    }

    private void placeGrave() {
        if (graveHandled) return;
        graveHandled = true;
        handleGravePlacement();
        discard();
    }

    private void handleGravePlacement() {
        BlockPos landingPos = findLandingPosition();

        BlockPos gravePos = voidRecovery ? landingPos : PlaceableUtils.getGraveStoneBlockPosition(level(), landingPos);

        String variantStr = variantId != null ? variantId.toString() : null;
        Block graveBlock = VariantUtils.getVariantId(variantStr, graveLevel);

        if (tryPlaceGrave(gravePos, graveBlock)) return;

        if (!level().isClientSide()) {
            if (!voidRecovery) {
                BlockPos altPos = PlaceableUtils.findP2P(level(), landingPos, 16);
                if (altPos != null && tryPlaceGrave(altPos, graveBlock)) return;
            }

            if (graveData == null) return;
            InventoryUtils.dropFullGrave(level(), landingPos, graveData);
        }
    }

    private boolean tryPlaceGrave(BlockPos pos, Block graveBlock) {
        boolean waterlogged = level().getFluidState(pos).isSourceOfType(Fluids.WATER);

        BlockState graveState = graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing).setValue(BlockStateProperties.WATERLOGGED, waterlogged);

        boolean placed = voidRecovery ? PlaceableUtils.placeGraveStoneExact(level(), pos, graveState) : PlaceableUtils.placeGraveStone(level(), pos, graveState);

        if (!placed) {
            return false;
        }

        if (!level().isClientSide()) {
            if (level().getBlockEntity(pos) instanceof GraveStoneBlockEntity blockEntity) {
                if (graveData != null) {
                    blockEntity.loadGraveData(graveData, level().registryAccess());
                }
                blockEntity.setVoidRecovery(voidRecovery);

                blockEntity.initializeGrave(ownerUUID, ownerName, System.currentTimeMillis(), null, null, graveLevel);

                if (variantId != null) {
                    blockEntity.setVariant(GraveVariantRegistry.get(variantId));
                }

                if (graveData != null && graveData.hasUUID(KEYS.getId()) && level() instanceof ServerLevel serverLevel) {
                    GraveDataManager.get(serverLevel).setGravePos(graveData.getUUID(KEYS.getId()), pos);
                }

                if (!blockEntity.isDecorative()) {
                    CemeteryManager.getInstance().addGrave(level().dimension(), pos);
                }
            }
            spawnLandingDust(pos);
            spawnLandingFogCycle(pos);
            level().levelEvent(2001, pos, Block.getId(graveState));
        }
        return true;
    }

    private void spawnLandingDust(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        String variantPath = variantId != null ? variantId.getPath() : null;
        float[] color = VariantUtils.getVariantColor(variantPath);

        float scale = 0.85f;
        int count = 18;
        double spread = 0.74;
        double ySpread = 0.028;
        double speed = 0.010;

        GroundDustParticleOptions options = new GroundDustParticleOptions(color[0], color[1], color[2], scale);
        serverLevel.sendParticles(options, pos.getX() + 0.5, pos.getY() + 0.02, pos.getZ() + 0.5, count, spread, ySpread, spread, speed);
    }

    private void spawnLandingFogCycle(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        GhostlyFogEntity fog = EntityRegistry.GHOSTLY_FOG.get().create(serverLevel);
        if (fog == null) {
            return;
        }

        String variantPath = variantId != null ? variantId.getPath() : null;
        float[] color = VariantUtils.getVariantColor(variantPath);
        int density = 12;
        float scale = 1.10f;

        double x = pos.getX() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 0.10;
        double y = pos.getY() - 0.06;
        double z = pos.getZ() + 0.5 + (serverLevel.random.nextDouble() - 0.5) * 0.10;

        fog.moveTo(x, y, z, 0f, 0f);
        fog.configure(3, 1.52f + serverLevel.random.nextFloat() * 0.12f, density, -scale, color[0], color[1], color[2]);
        serverLevel.addFreshEntity(fog);
    }

    private void updateChunkTickets() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos center = new ChunkPos(blockPosition());
        Set<Long> required = new HashSet<>();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                long chunkId = chunk.toLong();
                required.add(chunkId);

                if (!activeChunkTickets.contains(chunkId)) {
                    serverLevel.getChunkSource().addRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
                }
            }
        }

        for (Long chunkId : activeChunkTickets) {
            if (!required.contains(chunkId)) {
                ChunkPos chunk = new ChunkPos(chunkId);
                serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
            }
        }

        activeChunkTickets.clear();
        activeChunkTickets.addAll(required);
    }


    private BlockPos findLandingPosition() {
        if (voidRecovery) {
            BlockPos base = lastSafePos != null ? lastSafePos : BlockPos.containing(position());

            BlockPos dirtPos = new BlockPos(base.getX(), level().getMinBuildHeight(), base.getZ());

            level().setBlock(dirtPos, PlaceableUtils.getBlockForLevel((ServerLevel) level()), 3);

            return dirtPos.above();
        }

        BlockPos blockPos = BlockPos.containing(position());

        if (blockPos.getY() <= level().getMinBuildHeight() + 4 && lastSafePos != null) {
            blockPos = lastSafePos;
        }

        if (blockPos.getY() <= level().getMinBuildHeight() + 2) {
            return new BlockPos(blockPos.getX(), level().getMinBuildHeight(), blockPos.getZ());
        }

        BlockState state = level().getBlockState(blockPos);
        if (!state.isAir() && !state.canBeReplaced()) {
            for (int y = 0; y <= 5; y++) {
                BlockPos above = blockPos.above(y);


                BlockState aboveState = level().getBlockState(above);

                BlockState below = level().getBlockState(above.below());
                if ((aboveState.isAir() || aboveState.canBeReplaced()) && below.isSolid()) {
                    return above;
                }
            }
        }

        for (int y = 0; y <= 10; y++) {

            BlockPos below = blockPos.below(y);
            BlockState belowState = level().getBlockState(below);

            BlockState atPos = level().getBlockState(below.above());


            if (belowState.isSolid() && (atPos.isAir() || atPos.canBeReplaced())) {
                return below.above();
            }
        }

        return blockPos;
    }

    public void stopRotation() {
        this.rotationSpeed = 0f;
        this.entityData.set(DATA_ROT_SPEED, 0f);
    }

    public float getGraveRotation(float partialTick) {
        return Mth.rotLerp(partialTick, prevRotation, entityData.get(DATA_ROTATION));
    }

    public GraveStoneLevels getGraveLevel() {
        int ordinal = entityData.get(DATA_LEVEL);
        GraveStoneLevels[] levels = GraveStoneLevels.values();


        if (ordinal >= 0 && ordinal < levels.length) {
            return levels[ordinal];
        }
        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }

    public Direction getFacing() {
        return Direction.from2DDataValue(entityData.get(DATA_FACING));
    }


    @Nullable
    public ResourceLocation getVariantId() {
        String variantStr = entityData.get(DATA_VARIANT);
        if (!variantStr.isEmpty()) {
            return ResourceLocation.tryParse(variantStr);
        }
        return variantId;
    }

    @Nullable
    public UUID getGraveId() {
        if (graveData != null && graveData.hasUUID(KEYS.getId())) {
            return graveData.getUUID(KEYS.getId());
        }
        return null;
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(KEYS.getGraveData())) {
            this.graveData = tag.getCompound(KEYS.getGraveData());
        }

        if (tag.hasUUID(KEYS.getOwnerUuid())) {
            this.ownerUUID = tag.getUUID(KEYS.getOwnerUuid());
        }

        this.ownerName = tag.contains(KEYS.getOwnerName()) ? tag.getString(KEYS.getOwnerName()) : null;
        this.graveLevel = GraveStoneLevels.CODEC.byName(tag.getString(KEYS.getGraveLevel()), GraveStoneLevels.GRAVESTONE_LEVEL_1);
        this.facing = Direction.from2DDataValue(tag.getInt(KEYS.getFacing()));
        this.rotationSpeed = tag.getFloat(KEYS.getRotationSpeed());
        this.lifetime = tag.getInt(KEYS.getLifetime());
        this.voidRecovery = tag.getBoolean(KEYS.getVoidRecovery());

        if (tag.contains(KEYS.getVariantId())) {
            this.variantId = ResourceLocation.tryParse(tag.getString(KEYS.getVariantId()));
            entityData.set(DATA_VARIANT, tag.getString(KEYS.getVariantId()));
        }

        entityData.set(DATA_LEVEL, graveLevel.ordinal());
        entityData.set(DATA_ROTATION, tag.getFloat(KEYS.getRotation()));
        entityData.set(DATA_FACING, this.facing.get2DDataValue());
        entityData.set(DATA_ROT_SPEED, this.rotationSpeed);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (graveData != null) {
            tag.put(KEYS.getGraveData(), graveData);
        }
        if (ownerUUID != null) {
            tag.putUUID(KEYS.getOwnerUuid(), ownerUUID);
        }
        if (ownerName != null) {
            tag.putString(KEYS.getOwnerName(), ownerName);
        }
        tag.putString(KEYS.getGraveLevel(), graveLevel.getSerializedName());
        tag.putInt(KEYS.getFacing(), facing.get2DDataValue());
        tag.putFloat(KEYS.getRotationSpeed(), rotationSpeed);
        tag.putInt(KEYS.getLifetime(), lifetime);
        tag.putBoolean(KEYS.getVoidRecovery(), voidRecovery);
        tag.putFloat(KEYS.getRotation(), entityData.get(DATA_ROTATION));

        if (variantId != null) {
            tag.putString(KEYS.getVariantId(), variantId.toString());
        }
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
