package it.hurts.sskirillss.yagm.block.entity;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import it.hurts.sskirillss.yagm.data.gravedata.GraveData;
import it.hurts.sskirillss.yagm.data.gravedata.GraveDataManager;
import it.hurts.sskirillss.yagm.init.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.structure.cemetery.data.CemeterySavedData;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import it.hurts.sskirillss.yagm.util.ParticleUtils;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class GraveStoneBlockEntity extends BlockEntity {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private boolean silkTouchPickup = false;
    private boolean voidRecovery = false;
    private boolean cleaned = false;
    private int clientTicks = 0;

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

        InventoryUtils.restoreFromNBT(player, inventoryData, true);

        if (inventoryData.contains(KEYS.getTotalExperience())) {
            int xp = inventoryData.getInt(KEYS.getTotalExperience());
            if (xp > 0) {
                player.giveExperiencePoints(xp);
            }
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        this.inventoryData = new CompoundTag();

        removeFromGraveManager();
    }

    public void dropItems(Level level, BlockPos pos) {
        if (suppressDropsOnRemove) return;
        if (inventoryData != null && !inventoryData.isEmpty()) {

            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            for (ItemStack item : InventoryUtils.getAllItemsFromNBT(level.registryAccess(), inventoryData)) {
                if (!item.isEmpty()) {
                    Containers.dropItemStack(level, x, y, z, item);
                }
            }

            if (AccessoryLoader.hasAnyHandler() && inventoryData.contains(KEYS.getAccessories(), Tag.TAG_COMPOUND)) {
                AccessoryLoader.loadNBT(inventoryData.getCompound(KEYS.getAccessories()), level.registryAccess()).values()
                        .forEach(slots -> slots.values().forEach(item -> {
                            if (!item.isEmpty()) {
                                Containers.dropItemStack(level, x, y, z, item);
                            }
                        }));
            }

            if (inventoryData.contains(KEYS.getTotalExperience()) && level instanceof ServerLevel serverLevel) {
                int xp = inventoryData.getInt(KEYS.getTotalExperience());
                if (xp > 0) {
                    ExperienceOrb.award(serverLevel, new Vec3(x, y, z), xp);
                }
            }

            this.inventoryData = new CompoundTag();
        }

        removeFromGraveManager();
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


    public void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isPaused() || !mc.isWindowActive()) {
            return;
        }

        clientTicks++;

        if(level instanceof ClientLevel clientLevel) {
            if (getGraveLevel() == GraveStoneLevels.GRAVESTONE_LEVEL_4 && clientTicks % 10 == 0) {
                BlockPos pos = getBlockPos();
                IGraveVariant variant = getVariant();

                String variantPath = null;

                if (variant != null && variant.getId() != null) {
                    variantPath = variant.getId().getPath();
                }

                float[] baseColor = VariantUtils.getVariantColor(variantPath);

                for (int i = 0; i < 2; i++) {
                    double angle = clientLevel.random.nextDouble() * (Math.PI * 2.0);
                    double radius = Math.sqrt(clientLevel.random.nextDouble()) * 1.0;

                    double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
                    double y = pos.getY() - 0.15 + clientLevel.random.nextDouble() * 0.08;
                    double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;

                    float variance = 0.08f;
                    float r = Mth.clamp(baseColor[0] + (clientLevel.random.nextFloat() * 2 - 1) * variance, 0f, 1f);
                    float g = Mth.clamp(baseColor[1] + (clientLevel.random.nextFloat() * 2 - 1) * variance, 0f, 1f);
                    float b = Mth.clamp(baseColor[2] + (clientLevel.random.nextFloat() * 2 - 1) * variance, 0f, 1f);

                    clientLevel.addParticle(new GraveTrailParticleOptions(r, g, b, 0.55f), x, y, z, 0.0, 0.040 + clientLevel.random.nextDouble() * 0.015, 0.0);
                }
            }

            if (getGraveLevel() == GraveStoneLevels.GRAVESTONE_LEVEL_3 && clientTicks % 2 == 0) {
                IGraveVariant variant = getVariant();
                String variantPath = variant != null && variant.getId() != null ? variant.getId().getPath() : null;

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
                    BlockPos pos = getBlockPos();
                    BlockState state = getBlockState();
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

                        double x = pos.getX() + 0.5 + ox + (clientLevel.random.nextDouble() - 0.5) * 0.03;
                        double y = pos.getY() + lyOffset + clientLevel.random.nextDouble() * 0.04;
                        double z = pos.getZ() + 0.5 + oz + (clientLevel.random.nextDouble() - 0.5) * 0.03;

                        if (clientTicks % 5 == 0) {
                            clientLevel.addParticle(ParticleUtils.constructSimpleSpark(new Color(155 + level.getRandom().nextInt(100), level.getRandom().nextInt(100), 0), 0.15f, 5 + level.getRandom().nextInt(5), 0.85f), x, y, z, 0.0, 0.025, 0.0);
                        }
                    }
                }
            }
        }
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

        ResourceLocation fromPath= VariantIdFromPath(path);
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


