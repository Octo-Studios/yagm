package it.hurts.sskirillss.yagm.network.handlers;


import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.api.events.providers.IServerEvent;
import it.hurts.sskirillss.yagm.utils.ItemUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import it.hurts.sskirillss.yagm.data.GraveDataManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class InventoryHelper {


    public static void handlePlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        CompoundTag graveData = savePlayerInventory(player);

        //Event
        EventResult result = IServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            YAGMCommon.LOGGER.info("Player death event interrupted for: " + player.getUUID());
            return;
        }

        if (AccessoryManager.hasAnyHandler()) {
            AccessoryManager.clearAllAccessories(player);
        }
        
        player.getInventory().clearContent();
    }



    public static CompoundTag savePlayerInventory(Player player) {
        CompoundTag nbt = new CompoundTag();
        Inventory inventory = player.getInventory();

        nbt.putUUID("Id", UUID.randomUUID());
        nbt.putUUID("PlayerUuid", player.getUUID());
        nbt.putString("PlayerName", player.getName().getString());


        if (player.getLastDamageSource() != null) {
            nbt.putString("DeathCause", player.getLastDamageSource().getLocalizedDeathMessage(player).getString());
        } else {
            nbt.putString("DeathCause", "Unknown");
        }

        ItemUtils.saveInventory(player.registryAccess(), nbt, "MainInventory", inventory.items);
        ItemUtils.saveInventory(player.registryAccess(), nbt, "ArmorInventory", inventory.armor);
        ItemUtils.saveInventory(player.registryAccess(), nbt, "OffhandInventory", inventory.offhand);


        if (AccessoryManager.hasAnyHandler() && player instanceof ServerPlayer serverPlayer) {
            Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.collectAllAccessories(serverPlayer);
            CompoundTag accessoriesNBT = AccessoryManager.saveAllToNBT(allAccessories, player.registryAccess());
            if (!accessoriesNBT.isEmpty()) {
                nbt.put("Accessories", accessoriesNBT);
            }
        }

        nbt.putDouble("PosX", player.getX());
        nbt.putDouble("PosY", player.getY());
        nbt.putDouble("PosZ", player.getZ());
        nbt.putString("Dimension", player.level().dimension().location().toString());

        if (player instanceof ServerPlayer serverPlayer) {
            ServerLevel serverLevel = serverPlayer.serverLevel();
            GraveDataManager manager = GraveDataManager.get(serverLevel);
            UUID graveId = nbt.getUUID("Id");

            NonNullList<ItemStack> mainCopy = copyInventoryList(inventory.items);
            NonNullList<ItemStack> armorCopy = copyInventoryList(inventory.armor);
            NonNullList<ItemStack> offhandCopy = copyInventoryList(inventory.offhand);

            manager.putTransientGrave(graveId, mainCopy, armorCopy, offhandCopy);
        }

        return nbt;
    }

    public static void setInventory(Player player, CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return;

        Inventory inventory = player.getInventory();

        ItemUtils.readInventory(player.registryAccess(), nbt, "MainInventory", inventory.items);
        ItemUtils.readInventory(player.registryAccess(), nbt, "ArmorInventory", inventory.armor);
        ItemUtils.readInventory(player.registryAccess(), nbt, "OffhandInventory", inventory.offhand);

        inventory.setChanged();
    }


    public static NonNullList<ItemStack> getAllItemsFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        NonNullList<ItemStack> allItems = NonNullList.create();

        NonNullList<ItemStack> mainInventory = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armorInventory = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhandInventory = NonNullList.withSize(1, ItemStack.EMPTY);

        ItemUtils.readInventory(provider, nbt, "MainInventory", mainInventory);
        ItemUtils.readInventory(provider, nbt, "ArmorInventory", armorInventory);
        ItemUtils.readInventory(provider, nbt, "OffhandInventory", offhandInventory);

        for (ItemStack stack : mainInventory) {
            if (!stack.isEmpty()) {
                allItems.add(stack);
            }
        }
        for (ItemStack stack : armorInventory) {
            if (!stack.isEmpty()) {
                allItems.add(stack);
            }
        }
        for (ItemStack stack : offhandInventory) {
            if (!stack.isEmpty()) {
                allItems.add(stack);
            }
        }
        return allItems;
    }


    public static boolean dropItemList(Level level, BlockPos pos, NonNullList<ItemStack> items) {
        if (items == null || ! hasNonEmptyItems(items)) return false;
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, item);
            }
        }
        return true;
    }

    public static void restoreInventory(NonNullList<ItemStack> playerInv, NonNullList<ItemStack> graveInv, ServerPlayer player) {
        for (int i = 0; i < Math.min(graveInv.size(), playerInv.size()); i++) {
            ItemStack graveItem = graveInv.get(i);
            if (graveItem.isEmpty()) continue;

            if (playerInv.get(i).isEmpty()) {
                playerInv.set(i, graveItem.copy());
            } else {
                giveOrDropItem(player, graveItem.copy());
            }
        }
    }

    public static void restoreFromNBT(ServerPlayer player, CompoundTag data, boolean restoreAccessories) {
        NonNullList<ItemStack> mainItems = NonNullList.withSize(36, ItemStack.EMPTY);
        NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
        NonNullList<ItemStack> offhandItems = NonNullList.withSize(1, ItemStack.EMPTY);

        ItemUtils.readInventory(player.registryAccess(), data, "MainInventory", mainItems);
        ItemUtils.readInventory(player.registryAccess(), data, "ArmorInventory", armorItems);
        ItemUtils.readInventory(player.registryAccess(), data, "OffhandInventory", offhandItems);

        restoreInventory(player.getInventory().items, mainItems, player);
        restoreInventory(player.getInventory().armor, armorItems, player);
        restoreInventory(player.getInventory().offhand, offhandItems, player);

        if (restoreAccessories && AccessoryManager.hasAnyHandler() && data.contains("Accessories", 10)) {
            CompoundTag accessoriesNBT = data.getCompound("Accessories");
            Map<String, Map<String, ItemStack>> allAccessories = AccessoryManager.loadAllFromNBT(accessoriesNBT, player.registryAccess());
            AccessoryManager.restoreAllAccessories(player, allAccessories, true);
        } else {
            throw new RuntimeException();
        }
    }

    public static void giveOrDropItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return;
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    public static NonNullList<ItemStack> getOrThrowInventory(NonNullList<ItemStack> cached, Supplier<NonNullList<ItemStack>> supplier) {
        return cached != null ? cached : supplier.get();
    }

    public static boolean hasNonEmptyItems(NonNullList<ItemStack> items) {
        if (items == null || items.isEmpty()) return false;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return true;
        }
        return false;
    }

    public static NonNullList<ItemStack> copyInventoryList(NonNullList<ItemStack> source) {
        if (source == null) return null;
        NonNullList<ItemStack> copy = NonNullList.withSize(source.size(), ItemStack.EMPTY);
        IntStream.range(0, source.size()).forEach(i -> copy.set(i, source.get(i).copy()));
        return copy;
    }
}
