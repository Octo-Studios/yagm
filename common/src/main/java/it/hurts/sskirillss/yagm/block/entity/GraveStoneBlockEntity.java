package it.hurts.sskirillss.yagm.block.entity;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.init.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class GraveStoneBlockEntity extends BlockEntity {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private boolean silkTouchPickup = false;
    private boolean voidRecovery = false;
    private boolean cleaned = false;

    private static final String[] INVENTORY_KEYS = {
            KEYS.getMainInventory(), KEYS.getArmorInventory(), KEYS.getOffhandInventory(),
            KEYS.getAccessories(), KEYS.getTotalExperience()
    };

    private static final Map<String, GraveStoneLevels> LEVEL_PATTERNS = new LinkedHashMap<>();

    static {
        LEVEL_PATTERNS.put("tier_4", GraveStoneLevels.GRAVESTONE_LEVEL_4);
        LEVEL_PATTERNS.put("tier_3", GraveStoneLevels.GRAVESTONE_LEVEL_3);
        LEVEL_PATTERNS.put("tier_2", GraveStoneLevels.GRAVESTONE_LEVEL_2);
        LEVEL_PATTERNS.put("tier_1", GraveStoneLevels.GRAVESTONE_LEVEL_1);
    }

    @Getter
    private GraveData graveData = new GraveData();

    private CompoundTag inventoryData = new CompoundTag();
    private boolean suppressDropsOnRemove = false;

    public void setSuppressDropsOnRemove(boolean suppress) {
        this.suppressDropsOnRemove = suppress;
    }

    public void setVoidRecovery(boolean voidRecovery) {
        this.voidRecovery = voidRecovery;
        setChanged();
    }

    public boolean isVoidRecovery() {
        return voidRecovery;
    }

    public GraveStoneBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GRAVE_STONE.get(), pos, state);
    }

    public GraveStoneLevels getGraveLevel() {
        String path = getBlockPath();
        if (path != null) {
            GraveStoneLevels levels = LevelFromPath(path);
            if (levels != null) {
                return levels;
            }
        }

        return graveData.getGraveLevel();
    }

    public boolean isDecorative() {
        return graveData.isDecorative();
    }

    @Nullable
    public IGraveVariant getVariant() {
        ResourceLocation variantId = resolveVariantId();
        if (variantId == null) {
            return GraveVariantRegistry.getDefaultVariant();
        }

        IGraveVariant variant = GraveVariantRegistry.get(variantId);
        if (variant != null) {
            return variant;
        }

        return GraveVariantRegistry.getDefaultVariant();
    }

    public void setVariant(@Nullable IGraveVariant variant) {
        if (variant != null) {
            graveData.setVariantId(variant.getId());
        } else {
            graveData.setVariantId(null);
        }

        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void initializeGrave(UUID playerUUID, String playerName, long deathTime, @Nullable Component deathCause, @Nullable String testament, GraveStoneLevels graveLevel) {
        graveData.init(playerUUID, playerName, deathTime, deathCause, testament, graveLevel);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void loadGraveData(CompoundTag data, HolderLookup.Provider provider) {
        if (data.hasUUID(KEYS.getPlayerId())) {
            graveData.setOwnerUUID(data.getUUID(KEYS.getPlayerId()));
        }

        if (data.contains(KEYS.getPlayerName())) {
            graveData.setOwnerName(data.getString(KEYS.getPlayerName()));
        }

        if (data.hasUUID(KEYS.getId())) {
            graveData.setGraveId(data.getUUID(KEYS.getId()));
        }

        graveData.setDeathTime(data.getLong(KEYS.getDeathTime()));

        if (data.contains(KEYS.getDeathCause())) {
            try {
                graveData.setDeathCause(Component.Serializer.fromJson(data.getString(KEYS.getDeathCause()), provider));
            } catch (Exception e) {
                graveData.setDeathCause(Component.literal(data.getString(KEYS.getDeathCause())));
            }
        } else {
            graveData.setDeathCause(null);
        }

        if (data.contains(KEYS.getTestament())) {
            graveData.setTestament(data.getString(KEYS.getTestament()));
        } else {
            graveData.setTestament(null);
        }

        if (data.contains(KEYS.getVariantId())) {
            graveData.setVariantId(ResourceLocation.tryParse(data.getString(KEYS.getVariantId())));
        } else {
            graveData.setVariantId(null);
        }

        this.inventoryData = extractInventoryData(data);
        setChanged();
    }

    public void giveInventoryToPlayer(ServerPlayer player) {
        if (player == null || inventoryData == null || inventoryData.isEmpty()) {
            removeFromGraveManager();
            return;
        }

        InventoryUtils.restoreFullGrave(player, inventoryData, getBlockPos());
        this.inventoryData = new CompoundTag();
        syncToClient();

        removeFromGraveManager();
    }

    public void dropItems(Level level, BlockPos pos) {
        if (suppressDropsOnRemove) return;
        if (inventoryData != null && !inventoryData.isEmpty()) {
            InventoryUtils.dropFullGrave(level, pos, inventoryData);
            this.inventoryData = new CompoundTag();
        }

        removeFromGraveManager();
    }

    public void consumeByRestoreKey() {
        this.inventoryData = new CompoundTag();
        syncToClient();
        removeFromGraveManager();
    }

    private void syncToClient() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public CompoundTag getItemData() {
        CompoundTag tag = new CompoundTag();

        if (graveData.getOwnerUUID() != null) {
            tag.putUUID(KEYS.getPlayerId(), graveData.getOwnerUUID());
        }
        if (graveData.getOwnerName() != null) {
            tag.putString(KEYS.getPlayerName(), graveData.getOwnerName());
        }
        if (graveData.getGraveId() != null) {
            tag.putUUID(KEYS.getId(), graveData.getGraveId());
        }

        tag.putLong(KEYS.getDeathTime(), graveData.getDeathTime());

        if (graveData.getTestament() != null && !graveData.getTestament().isEmpty()) {
            tag.putString(KEYS.getTestament(), graveData.getTestament());
        }
        if (graveData.getVariantId() != null) {
            tag.putString(KEYS.getVariantId(), graveData.getVariantId().toString());
        }
        if (inventoryData != null) {
            tag.merge(inventoryData);
        }

        return tag;
    }

    public CompoundTag getBlockItemData(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        graveData.save(tag, provider);
        if (inventoryData != null && !inventoryData.isEmpty()) {
            tag.put(KEYS.getInventoryData(), inventoryData.copy());
        }
        return tag;
    }

    public void loadFromBlockItemData(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains(KEYS.getOwnerUuid()) || tag.contains(KEYS.getGraveId())) {
            this.graveData = GraveData.load(tag, provider);
            this.inventoryData = tag.contains(KEYS.getInventoryData()) ? tag.getCompound(KEYS.getInventoryData()).copy() : new CompoundTag();
        } else if (tag.contains(KEYS.getInventoryData())) {
            loadGraveData(tag.getCompound(KEYS.getInventoryData()), provider);
        }
        setChanged();
    }


    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        graveData.save(tag, registries);

        if (inventoryData != null) {
            tag.put(KEYS.getInventoryData(), inventoryData);
        }
        tag.putBoolean(KEYS.getVoidRecovery(), voidRecovery);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.getBoolean(KEYS.getSync())) {
            this.graveData = GraveData.load(tag, registries);
            if (tag.contains(KEYS.getInventoryData())) {
                this.inventoryData = tag.getCompound(KEYS.getInventoryData()).copy();
            }
            return;
        }

        this.graveData = GraveData.load(tag, registries);
        this.voidRecovery = tag.getBoolean(KEYS.getVoidRecovery());

        if (tag.contains(KEYS.getInventoryData())) {
            this.inventoryData = tag.getCompound(KEYS.getInventoryData()).copy();
        } else {
            this.inventoryData = new CompoundTag();
        }
    }

    public CompoundTag getInventoryData() {
        return inventoryData;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(KEYS.getSync(), true);
        graveData.save(tag, registries);
        if (inventoryData != null && !inventoryData.isEmpty()) {
            tag.put(KEYS.getInventoryData(), inventoryData.copy());
        }
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    private String getBlockPath() {
        if (level == null) {
            return null;
        }

        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(level.getBlockState(getBlockPos()).getBlock());

        if (key == null) {
            return null;
        }

        return key.getPath();
    }

    @Nullable
    private GraveStoneLevels LevelFromPath(String path) {
        for (var entry : LEVEL_PATTERNS.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Nullable
    private ResourceLocation VariantIdFromPath(String path) {
        for (var entry : VariantUtils.VARIANT_PREFIXES.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        if (path.contains("grave_tier_")) {
            return GraveVariantTypes.DEFAULT.getResourceLocation();
        }

        return null;
    }

    @Nullable
    private ResourceLocation resolveVariantId() {
        ResourceLocation variantId = graveData.getVariantId();
        if (variantId != null) {
            return variantId;
        }

        String path = getBlockPath();
        if (path == null) {
            return null;
        }

        ResourceLocation fromPath = VariantIdFromPath(path);
        if (fromPath != null) {
            graveData.setVariantId(fromPath);
        }

        return fromPath;
    }

    private CompoundTag extractInventoryData(CompoundTag source) {
        CompoundTag result = new CompoundTag();

        for (String key : INVENTORY_KEYS) {
            if (source.contains(key)) {
                result.put(key, source.get(key));
            }
        }

        return result;
    }

    private void removeFromGraveManager() {
        if (cleaned) {
            return;
        }

        cleaned = true;

        UUID graveId = graveData.getGraveId();
        if (level instanceof ServerLevel serverLevel && graveId != null) {
            GraveDataManager.get(serverLevel).removeGrave(graveId);
            CemeteryManager.getInstance().removeGrave(serverLevel.dimension(), getBlockPos());
            CemeterySavedData.markDirty(serverLevel);
        }
    }
}
