package it.hurts.sskirillss.yagm.network.handlers;


import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.events.ServerEvent;
import it.hurts.sskirillss.yagm.utils.ItemUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;

import java.util.UUID;
import java.util.stream.IntStream;

import it.hurts.sskirillss.yagm.data.GraveDataManager;
import net.minecraft.server.level.ServerLevel;

public class InventoryHelper {


    public static void handlePlayerDeath(ServerPlayer player) {
        if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }
        CompoundTag graveData = savePlayerInventory(player);

        //Event
        EventResult result = ServerEvent.ON_PLAYER_DEATH.invoker().onPlayerDeath(player, graveData);

        if (result.interruptsFurtherEvaluation()) {
            YAGMCommon.LOGGER.info("Player death event interrupted for: " + player.getUUID());
            return;
        }
        player.getInventory().clearContent();
    }



    public static CompoundTag savePlayerInventory(Player player) {
        CompoundTag nbt = new CompoundTag();
        Inventory inventory = player.getInventory();

        nbt.putUUID("Id", UUID.randomUUID());
        nbt.putUUID("PlayerUuid", player.getUUID());
        nbt.putString("PlayerName", player.getName().getString());

        ItemUtils.saveInventory(player.registryAccess(), nbt, "MainInventory", inventory.items);
        ItemUtils.saveInventory(player.registryAccess(), nbt, "ArmorInventory", inventory.armor);
        ItemUtils.saveInventory(player.registryAccess(), nbt, "OffhandInventory", inventory.offhand);

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

    private static NonNullList<ItemStack> copyInventoryList(NonNullList<ItemStack> source) {
        NonNullList<ItemStack> copy = NonNullList.withSize(source.size(), ItemStack.EMPTY);
        IntStream.range(0, source.size()).forEach(i -> copy.set(i, source.get(i).copy()));
        return copy;
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
}
