package it.hurts.sskirillss.yagm.blocks.gravestones;

import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.provider.IGravestoneTitlesProvider;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.client.titles.renderer.GravestoneTitles;
import it.hurts.sskirillss.yagm.data.GraveData;
import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.network.handlers.InventoryHelper;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;


public class GraveStoneBlockEntity extends BlockEntity implements IGravestoneTitlesProvider {

    @Getter
    private final GraveData graveData = new GraveData();

    private CompoundTag inventoryData;
    private NonNullList<ItemStack> playerMainSlots;
    private NonNullList<ItemStack> playerArmorSlots;
    private NonNullList<ItemStack> playerOffHandSlots;

    private GravestoneTitles gravestoneTitles;

    public GraveStoneBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.GRAVE_STONE.get(), pos, state);
        this.gravestoneTitles = GravestoneTitles.create();
    }

    public String getOwnerName() {
        return graveData.getOwnerNameOrDefault();
    }

    public GraveStoneLevels getGraveLevel() {
        return graveData.getGraveLevel();
    }

    @Nullable
    public IGraveVariant getVariant() {
        ResourceLocation variantId = graveData.getVariantId();
        if (variantId != null) {
            IGraveVariant variant = GraveVariantRegistry.get(variantId);
            if (variant != null) {
                return variant;
            }
        }
        return GraveVariantRegistry.getDefaultVariant();
    }

    public void setVariant(IGraveVariant variant) {
        if (variant != null) {
            graveData.setVariantId(variant.getId());
        } else {
            graveData.setVariantId(null);
        }
        setChanged();
        syncToClient();
    }

    public float getTextHeight() {
        return switch (graveData.getGraveLevel()) {
            case GRAVESTONE_LEVEL_1 -> 1.4F;
            case GRAVESTONE_LEVEL_2 -> 1.4F;
            case GRAVESTONE_LEVEL_3 -> 2.1F;
            case GRAVESTONE_LEVEL_4 -> 2.9F;
        };
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

    public void initializeGrave(UUID playerUUID, String playerName, long deathTime, @Nullable Component deathCause, @Nullable String testament, GraveStoneLevels level) {
        graveData.initialize(playerUUID, playerName, deathTime, deathCause, testament, level);
        updateTitles();
        setChanged();
        syncToClient();
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

        updateTitles();
        setChanged();
        syncToClient();

        if (level != null && !level.isClientSide()) {
            level.setBlockEntity(this);
        }
    }

    public void setDeathCause(Component deathCause) {
        graveData.setDeathCause(deathCause);
        updateTitles();
        setChanged();
        syncToClient();
    }


    protected void updateTitles() {
        GraveStoneLevels level = graveData.getGraveLevel();
        String owner = null;
        Long time = null;
        String will = null;

        if (level != null) {
            if (level.hasFeature(GraveStoneLevels.GraveFeature.OWNER_NAME)) {
                owner = getOwnerName();
            }
            if (level.hasFeature(GraveStoneLevels.GraveFeature.DEATH_TIME)) {
                time = graveData.getDeathTime();
            }
            if (level.hasFeature(GraveStoneLevels.GraveFeature.TESTAMENT)) {
                will = graveData.getTestament();
            }
        }
        this.gravestoneTitles = GravestoneTitles.forDeathWithLevel(owner, time, will);
    }

    @Override
    public GravestoneTitles getGravestoneTitles() {
        return this.gravestoneTitles;
    }

    @Override
    public GravestoneTitles setGravestoneTitles(GravestoneTitles titles) {
        this.gravestoneTitles = titles;
        return this.gravestoneTitles;
    }

    @Override
    public Direction getTitleFacing() {
        if (getBlockState().hasProperty(GraveStoneBlock.FACING)) {
            return getBlockState().getValue(GraveStoneBlock.FACING);
        }
        return Direction.NORTH;
    }

    @Override
    public float getTitleBaseScale() {
        return 0.025f;
    }

    @Override
    public float getTitleStartY() {
        return graveData.getTextHeight();
    }

    @Override
    public boolean shouldRenderTitles() {
        return gravestoneTitles != null && gravestoneTitles.getVisibleTitles().length > 0;
    }


    protected void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        graveData.saveToTag(tag);

        if (inventoryData != null) {
            tag.put("InventoryData", inventoryData);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        graveData.loadFromTag(tag);

        if (tag.contains("InventoryData", 10)) {
            this.inventoryData = tag.getCompound("InventoryData").copy();
        }

        updateTitles();
    }


    public void interact(Player player) {
        if (level != null && !level.isClientSide) {
            if (canPlayerOpen(player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    giveInventoryToPlayer(serverPlayer);
                    level.removeBlock(getBlockPos(), false);
                }
            }
        }
    }

    private boolean canPlayerOpen(Player player) {
        UUID ownerUUID = graveData.getOwnerUUID();
        if (ownerUUID == null) return true;
        return ownerUUID.equals(player.getUUID()) || player.hasPermissions(2);
    }


    public void setTransientInventory(NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        this.playerMainSlots = InventoryHelper.copyInventoryList(main);
        this.playerArmorSlots = InventoryHelper.copyInventoryList(armor);
        this.playerOffHandSlots = InventoryHelper.copyInventoryList(offhand);
    }

    public void giveInventoryToPlayer(ServerPlayer player) {
        if (player == null) return;

        UUID graveId = graveData.getGraveId();
        boolean inventoryGiven = false;


        if (level instanceof ServerLevel serverLevel) {
            GraveDataManager graveDataManager = GraveDataManager.get(serverLevel);
            boolean restored = false;

            boolean accessoriesRestored = false;
            if (AccessoryManager.hasAnyHandler() && inventoryData != null && inventoryData.contains("Accessories", 10)) {
                CompoundTag accessoriesNBT = inventoryData.getCompound("Accessories");
                Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.loadAllFromNBT(accessoriesNBT, player.serverLevel().registryAccess());
                AccessoryManager.restoreAllAccessories(player, allAccessories, true);
                accessoriesRestored = true;
            }

            if (graveId != null) {
                NonNullList<ItemStack> main = InventoryHelper.getOrThrowInventory(this.playerMainSlots, () -> graveDataManager.getTransientMain(graveId));
                NonNullList<ItemStack> armor = InventoryHelper.getOrThrowInventory(this.playerArmorSlots, () -> graveDataManager.getTransientArmor(graveId));
                NonNullList<ItemStack> offhand = InventoryHelper.getOrThrowInventory(this.playerOffHandSlots, () -> graveDataManager.getTransientOffhand(graveId));

                if (main != null && InventoryHelper.hasNonEmptyItems(main)) {
                    InventoryHelper.restoreInventory(player.getInventory().items, main, player);

                    if (armor != null && InventoryHelper.hasNonEmptyItems(armor)) {
                        InventoryHelper.restoreInventory(player.getInventory().armor, armor, player);
                    }

                    if (offhand != null && InventoryHelper.hasNonEmptyItems(offhand)) {
                        InventoryHelper.restoreInventory(player.getInventory().offhand, offhand, player);
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
                InventoryHelper.restoreFromNBT(player, inventoryData, false);
                player.getInventory().setChanged();
                inventoryGiven = true;
            }
        } else if (inventoryData != null && !inventoryData.isEmpty()) {
            InventoryHelper.restoreFromNBT(player, inventoryData, true);
            player.getInventory().setChanged();
            inventoryGiven = true;
        }

        if (inventoryGiven) {
            this.inventoryData = new CompoundTag();
            setChanged();
        }

        removeFromGraveManager();
    }

    public void dropItems(Level level, BlockPos pos) {
        boolean hasItems = false;
        UUID graveId = graveData.getGraveId();

        if (level instanceof ServerLevel serverLevel && graveId != null) {
            GraveDataManager graveDataManager = GraveDataManager.get(serverLevel);

            NonNullList<ItemStack> main = InventoryHelper.getOrThrowInventory(this.playerMainSlots, () -> graveDataManager.getTransientMain(graveId));
            NonNullList<ItemStack> armor = InventoryHelper.getOrThrowInventory(this.playerArmorSlots, () -> graveDataManager.getTransientArmor(graveId));
            NonNullList<ItemStack> offhand = InventoryHelper.getOrThrowInventory(this.playerOffHandSlots, () -> graveDataManager.getTransientOffhand(graveId));

            hasItems |= InventoryHelper.dropItemList(level, pos, main);
            hasItems |= InventoryHelper.dropItemList(level, pos, armor);
            hasItems |= InventoryHelper.dropItemList(level, pos, offhand);

            graveDataManager.removeTransientGrave(graveId);
            clearTransientInventories();
        }

        if (inventoryData != null && !inventoryData.isEmpty()) {
            NonNullList<ItemStack> items = InventoryHelper.getAllItemsFromNBT(level.registryAccess(), inventoryData);
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
            setChanged();
        }

        removeFromGraveManager();
    }


    private void removeFromGraveManager() {
        UUID graveId = graveData.getGraveId();
        if (level instanceof ServerLevel serverLevel && graveId != null) {
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
        }
    }


    private void clearTransientInventories() {
        this.playerMainSlots = null;
        this.playerArmorSlots = null;
        this.playerOffHandSlots = null;
    }

}