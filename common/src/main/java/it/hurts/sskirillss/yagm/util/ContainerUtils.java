package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContainerUtils {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    public static void restoreInventory(NonNullList<ItemStack> target, NonNullList<ItemStack> source, ServerPlayer player, BlockPos dropPos) {
        for (int i = 0; i < Math.min(source.size(), target.size()); i++) {
            ItemStack item = source.get(i);

            if (item.isEmpty()) continue;

            if (target.get(i).isEmpty()) {
                target.set(i, item.copy());
            } else {
                giveOrDropItem(player, item.copy(), dropPos);
            }
        }
    }

    public static void saveDroppedItems(CompoundTag nbt, Collection<ItemEntity> drops, HolderLookup.Provider provider) {
        List<ItemStack> items = new ArrayList<>();

        for (ItemEntity drop : drops) {
            if (drop == null || drop.getItem().isEmpty()) {
                continue;
            }

            items.add(drop.getItem().copy());
        }

        ItemUtils.saveItemList(provider, nbt, KEYS.getDroppedItems(), items);
    }


    public static void giveOrDropItem(ServerPlayer player, ItemStack stack, BlockPos dropPos) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            if (dropPos != null) {
                Containers.dropItemStack(player.level(), dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack);
            } else {
                player.drop(stack, false);
            }
        }
    }

    public static void restoreFullGrave(ServerPlayer player, CompoundTag data) {
        restoreFullGrave(player, data, null);
    }

    public static void restoreFullGrave(ServerPlayer player, CompoundTag data, BlockPos dropPos) {
        NBTReaderUtils.restoreFromNBT(player, data, true, dropPos);

        long xp = data.getLong(KEYS.getTotalExperience());
        if (xp > 0) {
            TierUtils.addExperience(player, xp);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void dropFullGrave(Level level, BlockPos pos, CompoundTag data) {

        for (ItemStack item : NBTReaderUtils.getAllItemsFromNBT(level.registryAccess(), data)) {
            if (!item.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
            }
        }

        if (AccessoryLoader.hasAnyHandler() && data.contains(KEYS.getAccessories(), Tag.TAG_COMPOUND)) {
            AccessoryLoader.loadNBT(data.getCompound(KEYS.getAccessories()), level.registryAccess()).values()
                    .forEach(slots -> slots.values().forEach(item -> {
                        if (!item.isEmpty()) {
                            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, item);
                        }
                    }));
        }

        if (data.contains(KEYS.getBackpacks(), Tag.TAG_COMPOUND)) {
            BackpackLoader.dropBackpacks(level, pos, BackpackLoader.loadNBT(data.getCompound(KEYS.getBackpacks()), level.registryAccess()));
        }

        if (level instanceof ServerLevel serverLevel) {
            long xp = data.getLong(KEYS.getTotalExperience());
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), (int) Math.min(Integer.MAX_VALUE, xp));
            }
        }
    }
}
