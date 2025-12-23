package it.hurts.sskirillss.yagm.blocks.gravestones;

import it.hurts.sskirillss.yagm.data.GraveDataManager;
import it.hurts.sskirillss.yagm.network.handlers.InventoryHelper;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class GraveStoneBlockEntity extends BlockEntity {

    @Getter
    private UUID ownerUUID;
    private String ownerName;
    @Getter
    private UUID graveId;
    @Getter
    private long deathTime;
    private CompoundTag inventoryData;
    private NonNullList<ItemStack> playerMainSlots;
    private NonNullList<ItemStack> playerArmorSlots;
    private NonNullList<ItemStack> playerOffHandSlots;

    public GraveStoneBlockEntity(BlockPos pos, BlockState state) {
        super(EntityRegistry.GRAVE_STONE.get(), pos, state);
        this.graveId = UUID.randomUUID();
        this.deathTime = System.currentTimeMillis();
    }

    public void loadGraveData(CompoundTag data) {

        if (data.hasUUID("PlayerUuid")) {
            this.ownerUUID = data.getUUID("PlayerUuid");
        }
        this.ownerName = data.getString("PlayerName");

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

        if (data.hasUUID("Id")) {
            this.graveId = data.getUUID("Id");
        }

        if (data.contains("DeathTime")) {
            this.deathTime = data.getLong("DeathTime");
        }

        this.setChanged();

        if (level != null && !level.isClientSide()) {
            level.setBlockEntity(this);
        }

    }

    public void interact(Player player) {
        if (level != null && !level.isClientSide) {
            if (canPlayerOpen(player)) {
                if (player instanceof ServerPlayer serverPlayer) {
                    restorePlayerInventory(serverPlayer);
                    level.removeBlock(getBlockPos(), false);
                }
            }
        }
    }

    private boolean canPlayerOpen(Player player) {
        if (ownerUUID == null) return true;
        return ownerUUID.equals(player.getUUID()) || player.hasPermissions(2);
    }

    private void restorePlayerInventory(ServerPlayer player) {
        if (inventoryData != null) {
            InventoryHelper.setInventory(player, inventoryData);
        }
    }


    private void copyItems(NonNullList<ItemStack> source, NonNullList<ItemStack> target) {
        IntStream.range(0, Math.min(source.size(), target.size()))
                .forEach(i -> target.set(i, source.get(i).copy()));
    }

    private NonNullList<ItemStack> getOrThrowInventory(NonNullList<ItemStack> cached, Supplier<NonNullList<ItemStack>> supplier) {
        return cached != null ? cached : supplier.get();
    }


    public void giveInventoryToPlayer(ServerPlayer player) {
        if (player == null) return;

        if (inventoryData == null || inventoryData.isEmpty()) {
            // Try to restore from transient storage even if inventoryData is empty
        }

        if (level instanceof ServerLevel serverLevel) {
            GraveDataManager graveDataManager = GraveDataManager.get(serverLevel);
            boolean restored = false;

            if (graveId != null) {
                NonNullList<ItemStack> main = getOrThrowInventory(this.playerMainSlots, () -> graveDataManager.getTransientMain(graveId));
                NonNullList<ItemStack> armor = getOrThrowInventory(this.playerArmorSlots, () -> graveDataManager.getTransientArmor(graveId));
                NonNullList<ItemStack> offhand = getOrThrowInventory(this.playerOffHandSlots, () -> graveDataManager.getTransientOffhand(graveId));

                if (main != null) {
                    copyItems(main, player.getInventory().items);

                    if (armor != null) {
                        copyItems(armor, player.getInventory().armor);
                    }

                    if (offhand != null) {
                        copyItems(offhand, player.getInventory().offhand);
                    }

                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();

                    graveDataManager.removeTransientGrave(graveId);
                    clearTransientInventories();
                    restored = true;
                }
            }

            if (!restored) {
                if (inventoryData != null && !inventoryData.isEmpty()) {
                    InventoryHelper.setInventory(player, inventoryData);
                    player.getInventory().setChanged();
                    restored = true;
                }
            }
        } else {
            if (inventoryData != null && !inventoryData.isEmpty()) {
                InventoryHelper.setInventory(player, inventoryData);
                player.getInventory().setChanged();
            }
        }


        this.inventoryData = new CompoundTag();
        this.setChanged();


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


    public void dropItems(Level level, BlockPos pos) {

        if (inventoryData == null || inventoryData.isEmpty()) return;

        NonNullList<ItemStack> items = InventoryHelper.getAllItemsFromNBT(level.registryAccess(), inventoryData);

        int droppedCount = 0;
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, item);
                droppedCount++;
            }
        }

        this.inventoryData = new CompoundTag();
        this.setChanged();

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
            }
        }
    }

    public String getOwnerName() {
        return ownerName != null ? ownerName : "Unknown Name";
    }

    public void setDeathTime(long deathTime) {
        this.deathTime = deathTime;
        this.setChanged();
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);

        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (ownerName != null) {
            tag.putString("OwnerName", ownerName);
        }
        tag.putUUID("GraveId", graveId);
        tag.putLong("DeathTime", deathTime);

        if (inventoryData != null) {
            tag.put("InventoryData", inventoryData);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.ownerName = tag.getString("OwnerName");
        this.graveId = tag.getUUID("GraveId");
        this.deathTime = tag.getLong("DeathTime");

        if (tag.contains("InventoryData", 10)) {
            this.inventoryData = tag.getCompound("InventoryData").copy();
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


    public void setTransientInventory(
            NonNullList<ItemStack> main,
            NonNullList<ItemStack> armor,
            NonNullList<ItemStack> offhand
    ) {
        this.playerMainSlots = copyInventoryList(main);
        this.playerArmorSlots = copyInventoryList(armor);
        this.playerOffHandSlots = copyInventoryList(offhand);
    }

    private NonNullList<ItemStack> copyInventoryList(NonNullList<ItemStack> source) {
        if (source == null) {
            return null;
        }
        NonNullList<ItemStack> copy = NonNullList.withSize(source.size(), ItemStack.EMPTY);
        IntStream.range(0, source.size()).forEach(i -> copy.set(i, source.get(i).copy()));

        return copy;
    }
}
