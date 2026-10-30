package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;


public class InventoryUtils {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private static final String[] KEY = {"MainInventory", "ArmorInventory", "OffhandInventory"};

    private static final int[] SIZES = {36, 4, 1};

    @SuppressWarnings("unchecked")
    public static CompoundTag savePlayerInventory(Player player) {
        CompoundTag nbt = new CompoundTag();
        Inventory inv = player.getInventory();
        var reg = player.registryAccess();

        nbt.putUUID(KEYS.getId(), UUID.randomUUID());
        nbt.putUUID(KEYS.getPlayerId(), player.getUUID());
        nbt.putString(KEYS.getPlayerName(), player.getName().getString());
        nbt.putString(KEYS.getDeathCause(), player.getLastDamageSource() != null ? player.getLastDamageSource().getLocalizedDeathMessage(player).getString() : "Unknown");

        NonNullList<ItemStack>[] lists = new NonNullList[]{inv.items, inv.armor, inv.offhand};

        for (int i = 0; i < KEY.length; i++) {
            ItemUtils.saveInventory(reg, nbt, KEY[i], withoutBlacklisted(lists[i]));
        }

        if (player instanceof ServerPlayer sp && AccessoryLoader.hasAnyHandler()) {
            CompoundTag acc = AccessoryLoader.saveNBT(AccessoryLoader.collectAccessories(sp), reg);
            if (!acc.isEmpty()) nbt.put(KEYS.getAccessories(), acc);
        }

        if (player instanceof ServerPlayer sp && BackpackLoader.hasHandler()) {
            CompoundTag backpacks = BackpackLoader.saveNBT(BackpackLoader.collectBackpacks(sp), reg);
            if (!backpacks.isEmpty()) nbt.put(KEYS.getBackpacks(), backpacks);
        }

        var experience = TierUtils.getTotalXpFromLevelAndProgress(player.experienceLevel, player.experienceProgress);

        if (ConfigRegistry.CONFIG.isStoreOnlyVanillaXp()) {
            experience = Math.min(experience, Math.min(player.experienceLevel * 7L, 100L));
        }

        nbt.putLong(KEYS.getTotalExperience(), Math.round(experience * ConfigRegistry.CONFIG.getXpRetentionPercent() / 100D));

        nbt.putDouble(KEYS.getDeathPosX(), player.getX());
        nbt.putDouble(KEYS.getDeathPosY(), player.getY());
        nbt.putDouble(KEYS.getDeathPosZ(), player.getZ());
        nbt.putString(KEYS.getDimension(), player.level().dimension().location().toString());

        return nbt;
    }

    private static NonNullList<ItemStack> withoutBlacklisted(NonNullList<ItemStack> source) {
        var result = NonNullList.withSize(source.size(), ItemStack.EMPTY);

        for (var i = 0; i < source.size(); i++) {
            if (!BlacklistUtils.isBlacklisted(source.get(i))) {
                result.set(i, source.get(i));
            }
        }

        return result;
    }

    public static boolean hasRecoverableItems(ServerPlayer player) {
        Inventory inventory = player.getInventory();

        for (ItemStack stack : inventory.items) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        for (ItemStack stack : inventory.armor) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        for (ItemStack stack : inventory.offhand) {
            if (!stack.isEmpty()) {
                return true;
            }
        }

        if (AccessoryLoader.hasAnyHandler() && !AccessoryLoader.collectAccessories(player).isEmpty()) {
            return true;
        }

        return BackpackLoader.hasHandler() && !BackpackLoader.collectBackpacks(player).isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static NonNullList<ItemStack>[] createLists() {
        return new NonNullList[]{
                NonNullList.withSize(SIZES[0], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[1], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[2], ItemStack.EMPTY)
        };
    }


    public static double getItemScore(ItemStack stack) {
        for (Map.Entry<String, Double> entry : ConfigRegistry.CONFIG.getValuableItems().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.substring(1)));
                if (stack.is(tag)) {
                    return entry.getValue();
                }
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(key);
                if (itemId != null && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                    return entry.getValue();
                }
            }
        }
        return 0.0;
    }
}
