package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
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
    private boolean graveManagerCleaned = false;

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
            syncClientData();
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
        GraveStoneLevels level;

        if (level().isClientSide()) {
            int ordinal = entityData.get(DATA_LEVEL);
            GraveStoneLevels[] levels = GraveStoneLevels.values();
            if (ordinal >= 0 && ordinal < levels.length) {
                level = levels[ordinal];
            } else {
                level = GraveStoneLevels.GRAVESTONE_LEVEL_1;
            }
        } else {
            level = graveData.getGraveLevel();
        }

        GraveStoneLevels inferred = inferLevelFromBoundBlock();
        if (inferred != null && inferred != GraveStoneLevels.GRAVESTONE_LEVEL_1) {
            if (!level().isClientSide()) {
                graveData.setGraveLevel(inferred);
            }
            return inferred;
        }

        return level;
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

        if (variantId == null) {
            variantId = inferVariantFromBoundBlock();
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

    @Nullable
    private GraveStoneLevels inferLevelFromBoundBlock() {
        BlockPos pos = getBoundPos();
        if (pos == null || level() == null) {
            return null;
        }

        var state = level().getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return null;
        }

        String path = key.getPath();
        if (path.contains("tier_4")) return GraveStoneLevels.GRAVESTONE_LEVEL_4;
        if (path.contains("tier_3")) return GraveStoneLevels.GRAVESTONE_LEVEL_3;
        if (path.contains("tier_2")) return GraveStoneLevels.GRAVESTONE_LEVEL_2;
        if (path.contains("tier_1")) return GraveStoneLevels.GRAVESTONE_LEVEL_1;

        return null;
    }

    @Nullable
    private ResourceLocation inferVariantFromBoundBlock() {
        BlockPos pos = getBoundPos();
        if (pos == null || level() == null) {
            return null;
        }

        var state = level().getBlockState(pos);
        if (state.isAir()) {
            return null;
        }

        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) {
            return null;
        }

        String path = key.getPath();
        if (path.startsWith("cold_")) return GraveVariantTypes.COLD.getResourceLocation();
        if (path.startsWith("hot_")) return GraveVariantTypes.HOT.getResourceLocation();
        if (path.startsWith("nether_")) return GraveVariantTypes.NETHER.getResourceLocation();
        if (path.startsWith("end_")) return GraveVariantTypes.END.getResourceLocation();
        if (path.startsWith("tropics_")) return GraveVariantTypes.TROPICS.getResourceLocation();
        if (path.startsWith("ocean_")) return GraveVariantTypes.OCEAN.getResourceLocation();
        if (path.contains("grave_tier_")) return GraveVariantTypes.DEFAULT.getResourceLocation();

        return null;
    }

    public float getTextHeight() {
        float base = switch (getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 1.4F;
            case GRAVESTONE_LEVEL_2 -> 1.4F;
            case GRAVESTONE_LEVEL_3 -> 2.3F;
            case GRAVESTONE_LEVEL_4 -> 3.2F;
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
        return switch (getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_2 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_3 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_4 -> 0xFFFFFFFF;
        };
    }

    public void initializeGrave(UUID playerUUID, String playerName, long deathTime, @Nullable net.minecraft.network.chat.Component deathCause, @Nullable String testament, GraveStoneLevels level) {
        graveData.initialize(playerUUID, playerName, deathTime, deathCause, testament, level);
        syncClientData();
        refreshDimensions();
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
            entityData.set(DATA_LEVEL, getGraveLevel().ordinal());
            ResourceLocation variantId = graveData.getVariantId();
            if (variantId == null) {
                variantId = inferVariantFromBoundBlock();
                if (variantId != null) {
                    graveData.setVariantId(variantId);
                }
            }
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
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        float height = switch (getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 1.5f;
            case GRAVESTONE_LEVEL_2 -> 1.8f;
            case GRAVESTONE_LEVEL_3 -> 2.0f;
            case GRAVESTONE_LEVEL_4 -> 2.0f;
        };
        return EntityDimensions.scalable(0.98f, height);
    }

    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_LEVEL.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            if (boundPos == null) {
                discard();
                return;
            }
            if (tickCount % 20 == 0 && level().getBlockState(boundPos).isAir()) {
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
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * (Math.PI * 2.0);
            double radius = Math.sqrt(random.nextDouble()) * 1.0;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double y = pos.getY() - 0.15 + random.nextDouble() * 0.08;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

            float variance = 0.08f;
            float r = clamp01(base[0] + (random.nextFloat() * 2 - 1) * variance);
            float g = clamp01(base[1] + (random.nextFloat() * 2 - 1) * variance);
            float b = clamp01(base[2] + (random.nextFloat() * 2 - 1) * variance);

            double vx = 0.0;
            double vy = 0.040 + random.nextDouble() * 0.015;
            double vz = 0.0;

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
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            giveInventoryToPlayer(serverPlayer);
            level().removeBlock(boundPos, false);
            if (level() instanceof ServerLevel serverLevel) {
                CemeterySavedData.markDirty(serverLevel);
            }
        }
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

                boolean hasAnyTransient = InventoryUtils.hasNonEmptyItems(main)
                        || InventoryUtils.hasNonEmptyItems(armor)
                        || InventoryUtils.hasNonEmptyItems(offhand);

                if (hasAnyTransient) {
                    if (main != null && InventoryUtils.hasNonEmptyItems(main)) {
                        InventoryUtils.restoreInventory(player.getInventory().items, main, player);
                    }

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

    /**
     * Steals one random non-empty item from this grave's inventory.
     * Used by GhostEntity when raiding graves at night.
     * Invalidates transient cache to ensure consistency.
     *
     * @return the stolen ItemStack, or ItemStack.EMPTY if nothing to steal
     */
    public ItemStack stealRandomItem() {
        if (inventoryData == null || inventoryData.isEmpty()) return ItemStack.EMPTY;

        String[] keys = {"MainInventory", "ArmorInventory", "OffhandInventory"};

        // Collect non-empty slot references: [keyIndex, tagIndex]
        record SlotRef(int keyIndex, int tagIndex) {}
        List<SlotRef> candidates = new java.util.ArrayList<>();

        for (int k = 0; k < keys.length; k++) {
            if (inventoryData.contains(keys[k], net.minecraft.nbt.Tag.TAG_LIST)) {
                net.minecraft.nbt.ListTag list = inventoryData.getList(keys[k], net.minecraft.nbt.Tag.TAG_COMPOUND);
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag slotTag = list.getCompound(i);
                    ItemStack item = ItemStack.parseOptional(level().registryAccess(), slotTag);
                    if (!item.isEmpty()) {
                        candidates.add(new SlotRef(k, i));
                    }
                }
            }
        }

        if (candidates.isEmpty()) return ItemStack.EMPTY;

        SlotRef chosen = candidates.get(random.nextInt(candidates.size()));
        String key = keys[chosen.keyIndex];
        net.minecraft.nbt.ListTag list = inventoryData.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
        CompoundTag slotTag = list.getCompound(chosen.tagIndex);
        ItemStack stolen = ItemStack.parseOptional(level().registryAccess(), slotTag);

        // Remove from NBT
        list.remove(chosen.tagIndex);

        // Invalidate transient cache (force NBT path next time)
        clearTransientInventories();
        if (level() instanceof ServerLevel serverLevel) {
            UUID graveId = graveData.getGraveId();
            if (graveId != null) {
                GraveDataManager.get(serverLevel).removeTransientGrave(graveId);
            }
        }

        return stolen;
    }

    private void removeFromGraveManager() {
        if (graveManagerCleaned) return;
        graveManagerCleaned = true;

        UUID graveId = graveData.getGraveId();
        if (level() instanceof ServerLevel serverLevel && graveId != null) {
            GraveDataManager manager = GraveDataManager.get(serverLevel);
            manager.removeGrave(graveId);
            manager.removeTransientGrave(graveId);

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

