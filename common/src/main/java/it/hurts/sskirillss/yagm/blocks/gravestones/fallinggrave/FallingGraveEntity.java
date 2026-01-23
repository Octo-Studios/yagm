package it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave;

import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import it.hurts.sskirillss.yagm.structures.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.utils.GraveStoneHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("deprecation")
public class FallingGraveEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_ROTATION = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_VARIANT = SynchedEntityData.defineId(FallingGraveEntity.class, EntityDataSerializers.STRING);

    private CompoundTag graveData;
    private UUID ownerUUID;
    private String ownerName;
    private GraveStoneLevels graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;
    private Direction facing = Direction.NORTH;
    @Nullable
    private ResourceLocation variantId;

    private float rotationSpeed;
    private int lifetime = 0;
    private static final int MAX_LIFETIME = 200;

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
        entity.rotationSpeed = 15f + level.random.nextFloat() * 10f;

        BlockPos blockPos = BlockPos.containing(position);
        IGraveVariant variant = GraveVariantRegistry.getFor(level, blockPos);
        if (variant != null) {
            entity.variantId = variant.getId();
            graveData.putString("VariantId", variant.getId().toString());
            entity.entityData.set(DATA_VARIANT, variant.getId().toString());
        }

        entity.entityData.set(DATA_LEVEL, graveLevel.ordinal());
        entity.entityData.set(DATA_ROTATION, 0f);

        return entity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_LEVEL, 0);
        builder.define(DATA_ROTATION, 0f);
        builder.define(DATA_VARIANT, "");
    }

    @Override
    public void tick() {
        super.tick();
        lifetime++;

        if (!onGround()) {
            float currentRotation = entityData.get(DATA_ROTATION);
            float newRotation = (currentRotation + rotationSpeed) % 360f;
            entityData.set(DATA_ROTATION, newRotation);
        }

        if (this.getY() < level().getMinBuildHeight() - 10) {
            if (level().dimension() == Level.END) {
                BlockPos safePos = findEndIslandPosition(blockPosition());
                this.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                this.setDeltaMovement(Vec3.ZERO);
                if (!level().isClientSide()) {
                    level().playSound(null, safePos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }
        }

        Vec3 motion = getDeltaMovement();
        setDeltaMovement(motion.x * 0.98, motion.y - 0.04, motion.z * 0.98);

        move(MoverType.SELF, getDeltaMovement());

        if (!level().isClientSide()) {
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

            if (!shouldPlace && lifetime > MAX_LIFETIME) {
                shouldPlace = true;
            }
            
            if (shouldPlace) {
                placeGrave();
            }
        }
    }

    private void placeGrave() {
        BlockPos landingPos = findActualLandingPosition();
        BlockPos gravePos = GraveStoneHelper.getGraveStoneBlockPosition(level(), landingPos);

        String variantStr = variantId != null ? variantId.toString() : null;
        Block graveBlock = BlockRegistry.getBlockForVariant(variantStr, graveLevel);
        BlockState graveState = graveBlock.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);

        if (GraveStoneHelper.placeGraveStone(level(), gravePos, graveState)) {
            if (level().getBlockEntity(gravePos) instanceof GraveStoneBlockEntity graveEntity) {
                if (graveData != null) {
                    graveEntity.loadGraveData(graveData);
                }
                graveEntity.initializeGrave(ownerUUID, ownerName, System.currentTimeMillis(), null, null, graveLevel);

                if (variantId != null) {
                    graveEntity.getGraveData().setVariantId(variantId);
                }
            }

            CemeteryManager.getInstance().addGrave(level().dimension(), gravePos);
        }

        discard();
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
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos bestPos = null;
        double bestDistance = Double.MAX_VALUE;

        for (int radius = 0; radius <= 200; radius += 4) {
            for (int x = -radius; x <= radius; x += 2) {
                for (int z = -radius; z <= radius; z += 2) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;

                    for (int y = 0; y < level().getMaxBuildHeight(); y++) {
                        mutable.set(deathPos.getX() + x, y, deathPos.getZ() + z);

                        BlockState block = level().getBlockState(mutable);
                        BlockState above = level().getBlockState(mutable.above());

                        if (block.isSolid() && (above.isAir() || above.canBeReplaced())) {
                            BlockPos candidate = mutable.above();

                            if (hasSpaceForGrave(candidate)) {
                                double distance = calculateDistance(deathPos, candidate);

                                if (distance < bestDistance) {
                                    bestDistance = distance;
                                    bestPos = candidate.immutable();

                                    if (distance < 50) {
                                        return bestPos;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (bestPos != null && radius > 20) {
                return bestPos;
            }
        }

        if (bestPos == null) {
            bestPos = findMainEndIslandPosition();
        }

        return bestPos != null ? bestPos : new BlockPos(0, 65, 0);
    }

    private BlockPos findMainEndIslandPosition() {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = -20; x <= 20; x++) {
            for (int z = -20; z <= 20; z++) {
                for (int y = 50; y < 100; y++) {
                    mutable.set(x, y, z);

                    BlockState block = level().getBlockState(mutable);
                    BlockState above = level().getBlockState(mutable.above());

                    if (block.isSolid() && above.isAir()) {
                        return mutable.above().immutable();
                    }
                }
            }
        }
        return null;
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

    public float getGraveRotation(float partialTick) {
        float current = entityData.get(DATA_ROTATION);
        if (!onGround()) {
            return current + rotationSpeed * partialTick;
        }
        return current;
    }

    public GraveStoneLevels getGraveLevel() {
        int ordinal = entityData.get(DATA_LEVEL);
        GraveStoneLevels[] levels = GraveStoneLevels.values();
        if (ordinal >= 0 && ordinal < levels.length) {
            return levels[ordinal];
        }
        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
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
    public IGraveVariant getVariant() {
        ResourceLocation id = getVariantId();
        if (id != null) {
            return GraveVariantRegistry.get(id);
        }
        return GraveVariantRegistry.getDefaultVariant();
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