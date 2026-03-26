package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.util.GraveStoneUtils;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.vec3.FallingGraveMotionConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private GraveStoneLevels graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;
    private Direction facing = Direction.NORTH;
    @Nullable
    private ResourceLocation variantId;

    private static final FallingGraveMotionConfig MOTION = FallingGraveMotionConfig.DEFAULT;

    private float rotationSpeed;
    private float prevRotation;
    private int lifetime = 0;
    private static final int CHUNK_TICKET_RADIUS = 2;
    private static final TicketType<Long> FALLING_GRAVE_TICKET = TicketType.create("yagm_falling_grave", Long::compareTo);
    private final Set<Long> activeChunkTickets = new HashSet<>();

    public FallingGraveEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static FallingGraveEntity create(Level level, Vec3 position, Vec3 velocity, CompoundTag graveData, GraveStoneLevels graveLevel, UUID ownerUUID, String ownerName, Direction facing) {
        FallingGraveEntity entity = new FallingGraveEntity(EntityRegistry.FALLING_GRAVE.get(), level);
        entity.setPos(position);
        entity.setDeltaMovement(velocity);
        entity.graveData = graveData;
        entity.graveLevel = graveLevel;
        entity.ownerUUID = ownerUUID;
        entity.ownerName = ownerName;
        entity.facing = facing;
        entity.rotationSpeed = MOTION.randomRotSpeed(level.random);

        BlockPos blockPos = BlockPos.containing(position);
        IGraveVariant variant = GraveVariantRegistry.getFor(level, blockPos);
        if (variant != null) {
            entity.variantId = variant.getId();
            graveData.putString("VariantId", variant.getId().toString());
            entity.entityData.set(DATA_VARIANT, variant.getId().toString());
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
            entityData.set(DATA_ROTATION, (prevRotation + rotationSpeed) % 360f);
        }

        if (!level().isClientSide()) {
            if (this.getY() < level().getMinBuildHeight() - 10 && level().dimension() == Level.END) {
                BlockPos safePos = findEndIslandPosition(blockPosition());
                this.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                this.setDeltaMovement(Vec3.ZERO);
                level().playSound(null, safePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            Vec3 motion = getDeltaMovement();
            setDeltaMovement(MOTION.applyPhysics(motion));
            move(MoverType.SELF, getDeltaMovement());

            boolean shouldPlace = onGround();

            if (!shouldPlace && motion.y < 0) {
                BlockPos belowPos = blockPosition().below();
                BlockState belowState = level().getBlockState(belowPos);

                if (belowState.isSuffocating(level(), belowPos) || belowState.isSolid()) {
                    BlockPos currentPos = blockPosition();
                    BlockState currentState = level().getBlockState(currentPos);

                    if (!currentState.isAir() && !currentState.canBeReplaced()) {
                        shouldPlace = true;
                    }
                }
            }

            if (!shouldPlace && lifetime > MOTION.getMaxLifetime()) {
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

    @Override
    public void remove(@NotNull RemovalReason reason) {
        if (!level().isClientSide()) {
            releaseChunkTickets();
        }
        super.remove(reason);
    }

    private void placeGrave() {
        BlockPos landingPos = findActualLandingPosition();
        BlockPos gravePos = GraveStoneUtils.getGraveStoneBlockPosition(level(), landingPos);

        String variantStr = variantId != null ? variantId.toString() : null;
        Block graveBlock = BlockRegistry.getBlockForVariant(variantStr, graveLevel);
        BlockState graveState = graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);

        if (GraveStoneUtils.placeGraveStone(level(), gravePos, graveState)) {
            if (!level().isClientSide()) {
                GraveStoneEntity graveEntity = GraveStoneEntity.create(level(), gravePos);
                if (graveData != null) {
                    graveEntity.loadGraveData(graveData);
                }
                graveEntity.initializeGrave(ownerUUID, ownerName, System.currentTimeMillis(), null, null, graveLevel);

                if (variantId != null) {
                    graveEntity.setVariant(GraveVariantRegistry.get(variantId));
                }
                level().addFreshEntity(graveEntity);
                spawnLandingDustBurst(gravePos);
                level().levelEvent(2001, gravePos, Block.getId(graveState));
            }
            CemeteryManager.getInstance().addGrave(level().dimension(), gravePos);
        } else if (!level().isClientSide()) {
            dropGraveDataAsItems(gravePos);
        }

        discard();
    }

    private void dropGraveDataAsItems(BlockPos pos) {
        if (graveData == null) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;

        NonNullList<ItemStack> items = InventoryUtils.getAllItemsFromNBT(level().registryAccess(), graveData);
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                Containers.dropItemStack(level(), x, y, z, item);
            }
        }

        if (AccessoryManager.hasAnyHandler() && graveData.contains("Accessories", 10)) {
            CompoundTag accessoriesNBT = graveData.getCompound("Accessories");
            Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.loadAllFromNBT(accessoriesNBT, level().registryAccess());
            for (Map<String, ItemStack> handlerAccessories : allAccessories.values()) {
                for (ItemStack accessory : handlerAccessories.values()) {
                    if (!accessory.isEmpty()) {
                        Containers.dropItemStack(level(), x, y, z, accessory);
                    }
                }
            }
        }
    }

    private void spawnLandingDustBurst(BlockPos gravePos) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        float[] base = getDustBaseColor();
        float baseScale = switch (graveLevel) {
            case GRAVESTONE_LEVEL_1 -> 1.0f;
            case GRAVESTONE_LEVEL_2 -> 1.0f;
            case GRAVESTONE_LEVEL_3 -> 1.0f;
            case GRAVESTONE_LEVEL_4 -> 1.0f;
        };

        for (int i = 0; i < 56; i++) {
            double x = gravePos.getX() + 0.5 + (random.nextDouble() * 3.0 - 1.5);
            double y = gravePos.getY() + 0.01 + random.nextDouble() * 0.04;
            double z = gravePos.getZ() + 0.5 + (random.nextDouble() * 3.0 - 1.5);

            float variance = 0.09f;
            float r = clamp01(base[0] + (random.nextFloat() * 2 - 1) * variance);
            float g = clamp01(base[1] + (random.nextFloat() * 2 - 1) * variance);
            float b = clamp01(base[2] + (random.nextFloat() * 2 - 1) * variance);
            float scale = baseScale * (0.9f + random.nextFloat() * 0.55f);

            GroundDustParticleOptions options = new GroundDustParticleOptions(r, g, b, scale);
            double vx = (random.nextDouble() * 2.0 - 1.0) * 0.03;
            double vy = random.nextDouble() * 0.01;
            double vz = (random.nextDouble() * 2.0 - 1.0) * 0.03;
            serverLevel.sendParticles(options, x, y, z, 0, vx, vy, vz, 1.0);
        }
    }

    private float[] getDustBaseColor() {
        String path = variantId != null ? variantId.getPath() : "default";
        return switch (path) {
            case "cold" -> new float[]{0.72f, 0.82f, 0.92f};
            case "hot" -> new float[]{0.96f, 0.57f, 0.28f};
            case "nether" -> new float[]{0.83f, 0.24f, 0.24f};
            case "end" -> new float[]{0.74f, 0.66f, 0.96f};
            case "ocean" -> new float[]{0.34f, 0.74f, 0.93f};
            case "tropics" -> new float[]{0.43f, 0.88f, 0.58f};
            default -> new float[]{0.82f, 0.82f, 0.82f};
        };
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private void updateChunkTickets() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ChunkPos center = new ChunkPos(blockPosition());
        Set<Long> required = new HashSet<>();

        for (int dx = -CHUNK_TICKET_RADIUS; dx <= CHUNK_TICKET_RADIUS; dx++) {
            for (int dz = -CHUNK_TICKET_RADIUS; dz <= CHUNK_TICKET_RADIUS; dz++) {
                ChunkPos chunk = new ChunkPos(center.x + dx, center.z + dz);
                long chunkId = chunk.toLong();
                required.add(chunkId);

                if (!activeChunkTickets.contains(chunkId)) {
                    serverLevel.getChunkSource().addRegionTicket(FALLING_GRAVE_TICKET, chunk, CHUNK_TICKET_RADIUS, chunkId);
                }
            }
        }

        for (Long chunkId : activeChunkTickets) {
            if (!required.contains(chunkId)) {
                ChunkPos chunk = new ChunkPos(chunkId);
                serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, CHUNK_TICKET_RADIUS, chunkId);
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
            serverLevel.getChunkSource().removeRegionTicket(FALLING_GRAVE_TICKET, chunk, CHUNK_TICKET_RADIUS, chunkId);
        }

        activeChunkTickets.clear();
    }

    private BlockPos findActualLandingPosition() {
        Vec3 currentPos = position();
        BlockPos blockPos = BlockPos.containing(currentPos);

        if (level().dimension() == Level.END && blockPos.getY() < level().getMinBuildHeight() + 5) {
            return findEndIslandPosition(blockPos);
        }

        BlockState currentState = level().getBlockState(blockPos);
        if (!currentState.isAir() && !currentState.canBeReplaced()) {
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

    private BlockPos findEndIslandPosition(BlockPos deathPos) {
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int radius = 0; radius <= 64; radius += 2) {
            for (int x = -radius; x <= radius; x += 2) {
                for (int z = -radius; z <= radius; z += 2) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;

                    int wx = deathPos.getX() + x;
                    int wz = deathPos.getZ() + z;

                    int surfaceY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
                    if (surfaceY <= level().getMinBuildHeight()) continue;

                    BlockPos candidate = new BlockPos(wx, surfaceY, wz);
                    if (hasSpaceForGrave(candidate)) {
                        double distance = calculateDistance(deathPos, candidate);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestPos = candidate;
                            if (distance < 16) return bestPos;
                        }
                    }
                }
            }
            if (bestPos != null && radius > 16) return bestPos;
        }

        if (bestPos == null) {
            int mainY = level().getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
            if (mainY > level().getMinBuildHeight()) {
                bestPos = new BlockPos(0, mainY, 0);
            }
        }

        return bestPos != null ? bestPos : new BlockPos(0, 65, 0);
    }

    private boolean hasSpaceForGrave(BlockPos pos) {
        for (int i = 0; i < 2; i++) {
            BlockState state = level().getBlockState(pos.above(i));
            if (!state.isAir() && !state.canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    private double calculateDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public float getGraveRotation() {
        return entityData.get(DATA_ROTATION);
    }

    /**
     * at the 0°/360° boundary.
     */
    public float getGraveRotation(float partialTick) {
        float speed = entityData.get(DATA_ROT_SPEED);
        if (speed == 0f) {
            speed = rotationSpeed;
        }

        double nowTicks = System.nanoTime() / 50_000_000.0;
        return (float) ((nowTicks * speed) % 360.0);
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
        if (tag.contains("GraveData")) {
            this.graveData = tag.getCompound("GraveData");
        }

        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }

        this.ownerName = tag.getString("OwnerName");
        this.graveLevel = GraveStoneLevels.CODEC.byName(tag.getString("GraveLevel"), GraveStoneLevels.GRAVESTONE_LEVEL_1);
        this.facing = Direction.from2DDataValue(tag.getInt("Facing"));
        this.rotationSpeed = tag.getFloat("RotationSpeed");
        this.lifetime = tag.getInt("Lifetime");

        if (tag.contains("VariantId")) {
            this.variantId = ResourceLocation.tryParse(tag.getString("VariantId"));
            entityData.set(DATA_VARIANT, tag.getString("VariantId"));
        }

        entityData.set(DATA_LEVEL, graveLevel.ordinal());
        entityData.set(DATA_ROTATION, tag.getFloat("Rotation"));
        entityData.set(DATA_FACING, this.facing.get2DDataValue());
        entityData.set(DATA_ROT_SPEED, this.rotationSpeed);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (graveData != null) {
            tag.put("GraveData", graveData);
        }
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (ownerName != null) {
            tag.putString("OwnerName", ownerName);
        }
        tag.putString("GraveLevel", graveLevel.getSerializedName());
        tag.putInt("Facing", facing.get2DDataValue());
        tag.putFloat("RotationSpeed", rotationSpeed);
        tag.putInt("Lifetime", lifetime);
        tag.putFloat("Rotation", entityData.get(DATA_ROTATION));

        if (variantId != null) {
            tag.putString("VariantId", variantId.toString());
        }
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

