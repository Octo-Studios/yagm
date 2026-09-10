package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.placement.GravePlacementResult;
import it.hurts.sskirillss.yagm.component.placement.GravePlacementService;
import it.hurts.sskirillss.yagm.component.placement.GravePositionResolver;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
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

    private static final String[] INVENTORY_KEYS = {
            KEYS.getMainInventory(), KEYS.getArmorInventory(), KEYS.getOffhandInventory(), KEYS.getAccessories(), KEYS.getTotalExperience()
    };

    private static final FallingGraveMotionConfig MOTION = FallingGraveMotionConfig.DEFAULT;

    private float rotationSpeed;
    private float prevRotation;
    private int lifetime = 0;
    private boolean graveHandled = false;
    private BlockPos lastSafePos = null;
    private static final TicketType<Long> FALLING_GRAVE_TICKET = TicketType.create("yagm_falling_grave", Long::compareTo);

    private final Set<Long> ticket = new HashSet<>();

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
        if (level instanceof ServerLevel) {
            entity.getChunkT();
        }

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

            if (!shouldPlace && lifetime > 1200 && canPlace()) {
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
            if (!(level() instanceof ServerLevel serverLevel) || ticket.isEmpty()) {
                return;
            }

            for (Long chunkId : ticket) {
                ChunkPos chunk = new ChunkPos(chunkId);
                serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
            }

            ticket.clear();
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
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (isGravePlacedTick(serverLevel)) {
            return;
        }

        BlockPos landingPos = GravePositionResolver.resolveFallingLanding(serverLevel, position(), lastSafePos, voidRecovery);
        BlockPos gravePos = voidRecovery ? landingPos : PlaceableUtils.getGraveStoneBlockPosition(serverLevel, landingPos);

        if (tryPlaceGrave(serverLevel, gravePos)) return;

        if (!level().isClientSide()) {
            if (!voidRecovery) {
                BlockPos altPos = PlaceableUtils.findNear(serverLevel, landingPos, 16);
                if (altPos != null && tryPlaceGrave(serverLevel, altPos)) return;
            }

            if (graveData == null) return;
            GravePlacementService.dropUnplacedGrave(serverLevel, landingPos, graveData);
        }
    }

    private boolean tryPlaceGrave(ServerLevel level, BlockPos pos) {
        CompoundTag dataToLoad = resolvePlacementData();
        if (dataToLoad == null) {
            return false;
        }

        GravePlacementResult result = GravePlacementService.placeFalling(level, pos, dataToLoad, graveLevel, ownerUUID, ownerName, facing, variantId, voidRecovery);
        if (!result.placed()) {
            return false;
        }

        BlockPos placedPos = result.pos() == null ? pos : result.pos();
        BlockState placedState = result.state() == null ? level.getBlockState(placedPos) : result.state();

        spawnLandingDust(placedPos);
        spawnLandingFogCycle(placedPos);
        level.levelEvent(2001, placedPos, Block.getId(placedState));

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

                if (!ticket.contains(chunkId)) {
                    serverLevel.getChunkSource().addRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
                }
            }
        }

        for (Long chunkId : ticket) {
            if (!required.contains(chunkId)) {
                ChunkPos chunk = new ChunkPos(chunkId);
                serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
            }
        }

        ticket.clear();
        ticket.addAll(required);
    }

    private void getChunkT() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos center = new ChunkPos(blockPosition());
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                long chunkId = chunk.toLong();
                if (ticket.add(chunkId)) {
                    serverLevel.getChunkSource().addRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
                }
            }
        }
    }

    private boolean isGravePlacedTick(ServerLevel serverLevel) {
        if (graveData == null || !graveData.hasUUID(KEYS.getId())) {
            return false;
        }

        UUID graveId = graveData.getUUID(KEYS.getId());
        GraveDataManager manager = GraveDataManager.get(serverLevel);
        BlockPos existingPos = manager.getGravePos(graveId);
        if (existingPos == null) {
            return false;
        }

        serverLevel.getChunk(existingPos.getX() >> 4, existingPos.getZ() >> 4);
        return serverLevel.getBlockEntity(existingPos) instanceof GraveStoneBlockEntity blockEntity && graveId.equals(blockEntity.getGraveData().getGraveId());
    }

    private boolean canPlace() {
        BlockPos current = blockPosition();
        BlockState atCurrent = level().getBlockState(current);
        if (!atCurrent.isAir() && !atCurrent.canBeReplaced()) {
            return true;
        }

        int minY = level().getMinBuildHeight();
        for (int offset = 1; offset <= 24; offset++) {
            BlockPos below = current.below(offset);
            if (below.getY() <= minY + 1) {
                return true;
            }
            if (level().getBlockState(below).isSolid()) {
                return true;
            }
        }

        return false;
    }


    private CompoundTag resolvePlacementData() {
        if (!(level() instanceof ServerLevel serverLevel) || graveData == null || !graveData.hasUUID(KEYS.getId()) || hasInventoryPayload(graveData)) {
            return graveData;
        }

        UUID graveId = graveData.getUUID(KEYS.getId());
        CompoundTag managerData = GraveDataManager.get(serverLevel).getGrave(graveId);
        if (!hasInventoryPayload(managerData)) {
            return graveData;
        }

        this.graveData = managerData;
        return managerData;
    }

    private static boolean hasInventoryPayload(CompoundTag data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        for (String key : INVENTORY_KEYS) {
            if (data.contains(key)) {
                return true;
            }
        }

        return false;
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
