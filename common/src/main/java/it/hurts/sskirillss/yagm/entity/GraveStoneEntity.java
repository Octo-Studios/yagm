package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.component.type.GraveStoneLevels;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class GraveStoneEntity extends Entity {

    private static final EntityDataAccessor<String> DATA_OWNER_NAME = SynchedEntityData.defineId(GraveStoneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(GraveStoneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_VARIANT = SynchedEntityData.defineId(GraveStoneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> DATA_BOUND_POS = SynchedEntityData.defineId(GraveStoneEntity.class, EntityDataSerializers.LONG);

    @Getter
    private final GraveData graveData = new GraveData();

    private CompoundTag inventoryData;
    private NonNullList<ItemStack> playerMainSlots;
    private NonNullList<ItemStack> playerArmorSlots;
    private NonNullList<ItemStack> playerOffHandSlots;

    private BlockPos boundPos = BlockPos.ZERO;

    public GraveStoneEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static GraveStoneEntity create(Level level, BlockPos pos) {
        GraveStoneEntity entity = new GraveStoneEntity(EntityRegistry.GRAVE_STONE.get(), level);
        entity.setBoundPos(pos);
        return entity;
    }

    public void setBoundPos(BlockPos pos) {
        this.boundPos = pos.immutable();
        this.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (!level().isClientSide()) {
            entityData.set(DATA_BOUND_POS, boundPos.asLong());
        }
    }

    public BlockPos getBoundPos() {
        if (level().isClientSide()) {
            return BlockPos.of(entityData.get(DATA_BOUND_POS));
        }
        return boundPos;
    }

    public boolean isBoundTo(BlockPos pos) {
        BlockPos current = getBoundPos();
        return current != null && current.equals(pos);
    }

    public String getOwnerName() {
        if (level().isClientSide()) {
            String name = entityData.get(DATA_OWNER_NAME);
            return (name == null || name.isEmpty()) ? "Unknown" : name;
        }
        return graveData.getOwnerNameOrDefault();
    }

    public GraveStoneLevels getGraveLevel() {
        if (level().isClientSide()) {
            int ordinal = entityData.get(DATA_LEVEL);
            GraveStoneLevels[] levels = GraveStoneLevels.values();
            if (ordinal >= 0 && ordinal < levels.length) {
                return levels[ordinal];
            }
        }
        return graveData.getGraveLevel();
    }

    @Nullable
    public IGraveVariant getVariant() {
        ResourceLocation variantId = graveData.getVariantId();
        if (level().isClientSide()) {
            String variantStr = entityData.get(DATA_VARIANT);
            if (variantStr != null && !variantStr.isEmpty()) {
                variantId = ResourceLocation.tryParse(variantStr);
            } else {
                variantId = null;
            }
        }
        if (variantId != null) {
            IGraveVariant variant = GraveVariantRegistry.get(variantId);
            if (variant != null) {
                return variant;
            }
        }
        return GraveVariantRegistry.getDefaultVariant();
    }

    public void setVariant(@Nullable IGraveVariant variant) {
        if (variant != null) {
            graveData.setVariantId(variant.getId());
        } else {
            graveData.setVariantId(null);
        }
        syncClientData();
    }

    public float getTextHeight() {
        float base = switch (graveData.getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 1.4F;
            case GRAVESTONE_LEVEL_2 -> 1.4F;
            case GRAVESTONE_LEVEL_3 -> 2.1F;
            case GRAVESTONE_LEVEL_4 -> 2.9F;
        };

        IGraveVariant variant = getVariant();
        if (variant != null) {
            base += variant.getTextHeightOffset();
        }
        return base;
    }

    public int getTextColor() {
        IGraveVariant variant = getVariant();
        if (variant != null) {
            return variant.getTextColor();
        }
        return switch (graveData.getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_2 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_3 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_4 -> 0xFFFFFFFF;
        };
    }

    public void initializeGrave(UUID playerUUID, String playerName, long deathTime, @Nullable net.minecraft.network.chat.Component deathCause, @Nullable String testament, GraveStoneLevels level) {
        graveData.initialize(playerUUID, playerName, deathTime, deathCause, testament, level);
        syncClientData();
    }

    public void loadGraveData(CompoundTag data) {
        graveData.loadFromGraveData(data);

        this.inventoryData = new CompoundTag();
        if (data.contains("MainInventory")) {
            this.inventoryData.put("MainInventory", data.get("MainInventory"));
        }
        if (data.contains("ArmorInventory")) {
            this.inventoryData.put("ArmorInventory", data.get("ArmorInventory"));
        }
        if (data.contains("OffhandInventory")) {
            this.inventoryData.put("OffhandInventory", data.get("OffhandInventory"));
        }
        if (data.contains("Accessories")) {
            this.inventoryData.put("Accessories", data.get("Accessories"));
        }

        syncClientData();
    }

    private void syncClientData() {
        if (!level().isClientSide()) {
            entityData.set(DATA_OWNER_NAME, getOwnerName());
            entityData.set(DATA_LEVEL, graveData.getGraveLevel().ordinal());
            ResourceLocation variantId = graveData.getVariantId();
            entityData.set(DATA_VARIANT, variantId != null ? variantId.toString() : "");
            if (boundPos != null) {
                entityData.set(DATA_BOUND_POS, boundPos.asLong());
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_OWNER_NAME, "");
        builder.define(DATA_LEVEL, GraveStoneLevels.GRAVESTONE_LEVEL_1.ordinal());
        builder.define(DATA_VARIANT, "");
        builder.define(DATA_BOUND_POS, 0L);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            if (boundPos == null) {
                discard();
                return;
            }
            if (level().getBlockState(boundPos).isAir()) {
                discard();
                return;
            }
        } else if (getGraveLevel() == GraveStoneLevels.GRAVESTONE_LEVEL_4) {
            spawnLoopingTier4Trails();
        }

        BlockPos pos = getBoundPos();
        if (pos != null) {
            double targetX = pos.getX() + 0.5D;
            double targetY = pos.getY();
            double targetZ = pos.getZ() + 0.5D;
            if (getX() != targetX || getY() != targetY || getZ() != targetZ) {
                setPos(targetX, targetY, targetZ);
            }
        }
    }

    private void spawnLoopingTier4Trails() {
        if (!(level() instanceof ClientLevel clientLevel) || tickCount % 10 != 0) {
            return;
        }

        BlockPos pos = getBoundPos();
        if (pos == null) {
            return;
        }

        float[] base = getTier4TrailBaseColor();
        for (int i = 0; i < 6; i++) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * 1.5;
            double y = pos.getY() - 0.15 + random.nextDouble() * 0.08;
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 2.0 - 1.0) * 1.5;

            float variance = 0.08f;
            float r = clamp01(base[0] + (random.nextFloat() * 2 - 1) * variance);
            float g = clamp01(base[1] + (random.nextFloat() * 2 - 1) * variance);
            float b = clamp01(base[2] + (random.nextFloat() * 2 - 1) * variance);

            double vx = (random.nextDouble() * 2.0 - 1.0) * 0.02;
            double vy = 0.20 + random.nextDouble() * 0.08;
            double vz = (random.nextDouble() * 2.0 - 1.0) * 0.02;

            clientLevel.addParticle(new GraveTrailParticleOptions(r, g, b, 0.55f), x, y, z, vx, vy, vz);
        }
    }

    private float[] getTier4TrailBaseColor() {
        IGraveVariant variant = getVariant();
        String path = variant != null && variant.getId() != null ? variant.getId().getPath() : "default";

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

    public void interact(Player player) {
        if (!level().isClientSide) {
            if (canPlayerOpen(player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    giveInventoryToPlayer(serverPlayer);
                    level().removeBlock(boundPos, false);
                    if (level() instanceof ServerLevel serverLevel) {
                        CemeterySavedData.markDirty(serverLevel);
                    }
                }
            }
        }
    }

    private boolean canPlayerOpen(Player player) {
        UUID ownerUUID = graveData.getOwnerUUID();
        if (ownerUUID == null) return true;
        return ownerUUID.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public void giveInventoryToPlayer(ServerPlayer player) {
        if (player == null) return;

        UUID graveId = graveData.getGraveId();
        boolean inventoryGiven = false;

        if (level() instanceof ServerLevel serverLevel) {
            GraveDataManager graveDataManager = GraveDataManager.get(serverLevel);
            boolean restored = false;

            if (AccessoryManager.hasAnyHandler() && inventoryData != null && inventoryData.contains("Accessories", 10)) {
                CompoundTag accessoriesNBT = inventoryData.getCompound("Accessories");
                Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.loadAllFromNBT(accessoriesNBT, player.serverLevel().registryAccess());
                AccessoryManager.restoreAllAccessories(player, allAccessories, true);
            }

            if (graveId != null) {
                NonNullList<ItemStack> main = InventoryUtils.getOrThrowInventory(this.playerMainSlots, () -> graveDataManager.getTransientMain(graveId));
                NonNullList<ItemStack> armor = InventoryUtils.getOrThrowInventory(this.playerArmorSlots, () -> graveDataManager.getTransientArmor(graveId));
                NonNullList<ItemStack> offhand = InventoryUtils.getOrThrowInventory(this.playerOffHandSlots, () -> graveDataManager.getTransientOffhand(graveId));

                if (main != null && InventoryUtils.hasNonEmptyItems(main)) {
                    InventoryUtils.restoreInventory(player.getInventory().items, main, player);

                    if (armor != null && InventoryUtils.hasNonEmptyItems(armor)) {
                        InventoryUtils.restoreInventory(player.getInventory().armor, armor, player);
                    }

                    if (offhand != null && InventoryUtils.hasNonEmptyItems(offhand)) {
                        InventoryUtils.restoreInventory(player.getInventory().offhand, offhand, player);
                    }

                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();

                    graveDataManager.removeTransientGrave(graveId);
                    clearTransientInventories();
                    restored = true;
                    inventoryGiven = true;
                }
            }

            if (!restored && inventoryData != null && !inventoryData.isEmpty()) {
                InventoryUtils.restoreFromNBT(player, inventoryData, false);
                player.getInventory().setChanged();
                inventoryGiven = true;
            }
        } else if (inventoryData != null && !inventoryData.isEmpty()) {
            InventoryUtils.restoreFromNBT(player, inventoryData, true);
            player.getInventory().setChanged();
            inventoryGiven = true;
        }

        if (inventoryGiven) {
            this.inventoryData = new CompoundTag();
        }

        removeFromGraveManager();
    }

    public void dropItems(Level level, BlockPos pos) {
        boolean hasItems = false;
        UUID graveId = graveData.getGraveId();

        if (level instanceof ServerLevel serverLevel && graveId != null) {
            GraveDataManager graveDataManager = GraveDataManager.get(serverLevel);

            NonNullList<ItemStack> main = InventoryUtils.getOrThrowInventory(this.playerMainSlots, () -> graveDataManager.getTransientMain(graveId));
            NonNullList<ItemStack> armor = InventoryUtils.getOrThrowInventory(this.playerArmorSlots, () -> graveDataManager.getTransientArmor(graveId));
            NonNullList<ItemStack> offhand = InventoryUtils.getOrThrowInventory(this.playerOffHandSlots, () -> graveDataManager.getTransientOffhand(graveId));

            hasItems |= InventoryUtils.dropItemList(level, pos, main);
            hasItems |= InventoryUtils.dropItemList(level, pos, armor);
            hasItems |= InventoryUtils.dropItemList(level, pos, offhand);

            graveDataManager.removeTransientGrave(graveId);
            clearTransientInventories();
        }

        if (inventoryData != null && !inventoryData.isEmpty()) {
            NonNullList<ItemStack> items = InventoryUtils.getAllItemsFromNBT(level.registryAccess(), inventoryData);
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    hasItems = true;
                    Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, item);
                }
            }

            if (AccessoryManager.hasAnyHandler() && inventoryData.contains("Accessories", 10)) {
                CompoundTag accessoriesNBT = inventoryData.getCompound("Accessories");
                Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.loadAllFromNBT(accessoriesNBT, level.registryAccess());

                for (Map<String, ItemStack> handlerAccessories : allAccessories.values()) {
                    for (ItemStack accessory : handlerAccessories.values()) {
                        if (!accessory.isEmpty()) {
                            hasItems = true;
                            Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, accessory);
                        }
                    }
                }
            }

            this.inventoryData = new CompoundTag();
        }

        if (hasItems) {
            // keep entity state consistent
        }

        removeFromGraveManager();
    }

    private void removeFromGraveManager() {
        UUID graveId = graveData.getGraveId();
        if (level() instanceof ServerLevel serverLevel && graveId != null) {
            GraveDataManager manager = GraveDataManager.get(serverLevel);
            var all = manager.getAllGraves();
            UUID keyToRemove = null;
            for (var entry : all.entrySet()) {
                CompoundTag tag = entry.getValue();
                if (tag != null && tag.hasUUID("Id") && tag.getUUID("Id").equals(graveId)) {
                    keyToRemove = entry.getKey();
                    break;
                }
            }
            if (keyToRemove != null) {
                manager.removeGrave(keyToRemove);
                manager.removeTransientGrave(graveId);
            }

            CemeteryManager.getInstance().removeGrave(serverLevel.dimension(), boundPos);

            CemeterySavedData.markDirty(serverLevel);
        }
    }

    private void clearTransientInventories() {
        this.playerMainSlots = null;
        this.playerArmorSlots = null;
        this.playerOffHandSlots = null;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("GraveData", 10)) {
            graveData.loadFromTag(tag.getCompound("GraveData"));
        }
        if (tag.contains("InventoryData", 10)) {
            this.inventoryData = tag.getCompound("InventoryData").copy();
        }
        if (tag.contains("BoundPos")) {
            this.boundPos = BlockPos.of(tag.getLong("BoundPos"));
        }
        if (boundPos != null) {
            setPos(boundPos.getX() + 0.5D, boundPos.getY(), boundPos.getZ() + 0.5D);
        }
        syncClientData();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        CompoundTag dataTag = new CompoundTag();
        graveData.saveToTag(dataTag);
        tag.put("GraveData", dataTag);

        if (inventoryData != null) {
            tag.put("InventoryData", inventoryData);
        }
        if (boundPos != null) {
            tag.putLong("BoundPos", boundPos.asLong());
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

