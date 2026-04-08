package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.*;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.HashSet;
import java.util.Map;
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
                spawnFlyingParticles();
            }
        }
    }

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide() && !graveHandled && reason == RemovalReason.KILLED) {
            graveHandled = true;
            handleGravePlacement();
        }
        if (!level().isClientSide()) {
            releaseChunkTickets();
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
        Block graveBlock = BlockRegistry.getVariant(variantStr, graveLevel);

        if (tryPlaceGrave(gravePos, graveBlock)) return;

        if (!level().isClientSide()) {
            if (!voidRecovery) {
                BlockPos altPos = PlaceableUtils.findP2P(level(), landingPos, 16);
                if (altPos != null && tryPlaceGrave(altPos, graveBlock)) return;
            }

            if (graveData == null) return;
            double x = landingPos.getX() + 0.5;
            double y = landingPos.getY() + 0.5;
            double z = landingPos.getZ() + 0.5;

            NonNullList<ItemStack> items = InventoryUtils.getAllItemsFromNBT(level().registryAccess(), graveData);
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    Containers.dropItemStack(level(), x, y, z, item);
                }
            }

            if (AccessoryLoader.hasAnyHandler() && graveData.contains(KEYS.getAccessories(), 10)) {
                CompoundTag accessoriesNBT = graveData.getCompound(KEYS.getAccessories());
                Map<String, Map<String, ItemStack>> allAccessories = AccessoryLoader.loadNBT(accessoriesNBT, level().registryAccess());
                for (Map<String, ItemStack> handlerAccessories : allAccessories.values()) {
                    for (ItemStack accessory : handlerAccessories.values()) {
                        if (!accessory.isEmpty()) {
                            Containers.dropItemStack(level(), x, y, z, accessory);
                        }
                    }
                }
            }

            int xp = graveData.getInt(KEYS.getTotalExperience());

            if (xp <= 0) return;

            if (level() instanceof ServerLevel serverLevel) {
                ExperienceOrb.award(serverLevel, Vec3.atCenterOf(landingPos), xp);
            }
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

                if (!blockEntity.isDecorative()) {
                    CemeteryManager.getInstance().addGrave(level().dimension(), pos);
                }
            }
            spawnLandingDustBurst(pos);
            level().levelEvent(2001, pos, Block.getId(graveState));
        }
        return true;
    }


    private void spawnLandingDustBurst(BlockPos gravePos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float[] base = VariantUtils.getVariantColor(variantId != null ? variantId.getPath() : null);
        float baseScale = 1.0f;

        for (int i = 0; i < 56; i++) {
            double x = gravePos.getX() + 0.5 + (random.nextDouble() * 3.0 - 1.5);
            double y = gravePos.getY() + 0.01 + random.nextDouble() * 0.04;
            double z = gravePos.getZ() + 0.5 + (random.nextDouble() * 3.0 - 1.5);

            float variance = 0.09f;
            float r = Mth.clamp(base[0] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
            float g = Mth.clamp(base[1] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
            float b = Mth.clamp(base[2] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
            float scale = baseScale * (0.9f + random.nextFloat() * 0.55f);

            GroundDustParticleOptions options = new GroundDustParticleOptions(r, g, b, scale);
            double vx = (random.nextDouble() * 2.0 - 1.0) * 0.03;
            double vy = random.nextDouble() * 0.01;
            double vz = (random.nextDouble() * 2.0 - 1.0) * 0.03;
            serverLevel.sendParticles(options, x, y, z, 0, vx, vy, vz, 1.0);
        }
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

    private void releaseChunkTickets() {
        if (!(level() instanceof ServerLevel serverLevel) || activeChunkTickets.isEmpty()) {
            return;
        }

        for (Long chunkId : activeChunkTickets) {
            ChunkPos chunk = new ChunkPos(chunkId);
            serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, 2, chunkId);
        }

        activeChunkTickets.clear();
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


    private void spawnFlyingParticles() {
        GraveStoneLevels graveLevel = getGraveLevel();
        ResourceLocation variantId = getVariantId();
        String variantPath = variantId != null ? variantId.getPath() : null;

        float rotation = entityData.get(DATA_ROTATION);
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        if (graveLevel == GraveStoneLevels.GRAVESTONE_LEVEL_4 && tickCount % 10 == 0) {
            float[] base = VariantUtils.getVariantColor(variantPath);
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * (Math.PI * 2.0);
                double radius = Math.sqrt(random.nextDouble());
                double x = getX() + Math.cos(angle) * radius;
                double y = getY() - 0.15 + random.nextDouble() * 0.08;
                double z = getZ() + Math.sin(angle) * radius;
                float variance = 0.08f;
                float r = Mth.clamp(base[0] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
                float g = Mth.clamp(base[1] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
                float b = Mth.clamp(base[2] + (random.nextFloat() * 2 - 1) * variance, 0f, 1f);
                level().addParticle(new GraveTrailParticleOptions(r, g, b, 0.55f), x, y, z, 0.0, 0.040 + random.nextDouble() * 0.015, 0.0);
            }
        }

        if (graveLevel == GraveStoneLevels.GRAVESTONE_LEVEL_3 && tickCount % 2 == 0) {

            double[][] candles;


            if ("end".equals(variantPath)) {
                candles = new double[][]{{0.375, -0.34375, 0.625}, {-0.375, -0.34375, 0.46875}};
            } else if ("hot".equals(variantPath)) {
                candles = new double[][]{{-0.34375, -0.1875, 0.625}, {-0.375, -0.390625, 0.46875}};
            } else if ("tropics".equals(variantPath)) {
                candles = new double[][]{{0.3125, -0.1875, 0.5625}, {0.28125, -0.390625, 0.40625}};
            } else {
                candles = null;
            }

            if (candles != null) {
                Direction facing = getFacing();
                for (double[] candle : candles) {
                    double lx = candle[0];
                    double lz = candle[1];
                    double lyOffset = candle[2];

                    double ox, oz;
                    switch (facing) {
                        case SOUTH -> {ox = -lx; oz = -lz;}
                        case EAST -> {ox = -lz; oz = lx;}
                        case WEST -> {ox = lz; oz = -lx;}
                        default -> {ox = lx; oz = lz;}
                    }

                    double rx = cos * ox - sin * oz;
                    double rz = sin * ox + cos * oz;

                    double x = getX() + rx + (random.nextDouble() - 0.5) * 0.03;
                    double y = getY() + lyOffset + random.nextDouble() * 0.04;

                    double z = getZ() + rz + (random.nextDouble() - 0.5) * 0.03;

                    level().addParticle(ParticleUtils.constructSimpleSpark(new Color(155 + random.nextInt(100), random.nextInt(100), 0), 0.15f, 5 + random.nextInt(5), 0.85f), x, y, z, 0.0, 0.025, 0.0);
                }
            }
        }
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