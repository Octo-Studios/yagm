package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
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

import java.util.*;

@Slf4j
public class InventoryUtils {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private static final String[] KEY = {"MainInventory", "ArmorInventory", "OffhandInventory"};
    private static final int[] SIZES = {36, 4, 1};

    public static CompoundTag savePlayerInventory(Player player) {
        CompoundTag nbt = new CompoundTag();
        Inventory inv = player.getInventory();
        var reg = player.registryAccess();

        nbt.putUUID(KEYS.getId(), UUID.randomUUID());
        nbt.putUUID(KEYS.getPlayerId(), player.getUUID());
        nbt.putString(KEYS.getPlayerName(), player.getName().getString());
        nbt.putString(KEYS.getDeathCause(), player.getLastDamageSource() != null ? player.getLastDamageSource().getLocalizedDeathMessage(player).getString() : "Unknown");

        NonNullList<ItemStack>[] lists = new NonNullList[] {inv.items, inv.armor, inv.offhand};

        for (int i = 0; i < KEY.length; i++) {
            ItemUtils.saveInventory(reg, nbt, KEY[i], lists[i]);
        }

        if (player instanceof ServerPlayer sp && AccessoryLoader.hasAnyHandler()) {
            CompoundTag acc = AccessoryLoader.saveNBT(AccessoryLoader.collectAccessories(sp), reg);
            if (!acc.isEmpty()) nbt.put(KEYS.getAccessories(), acc);
        }

        nbt.putInt(KEYS.getTotalExperience(), getTotalXpFromLevelAndProgress(player.experienceLevel, player.experienceProgress));

        nbt.putDouble(KEYS.getDeathPosX(), player.getX());
        nbt.putDouble(KEYS.getDeathPosY(), player.getY());
        nbt.putDouble(KEYS.getDeathPosZ(), player.getZ());
        nbt.putString(KEYS.getDimension(), player.level().dimension().location().toString());

        return nbt;
    }

    public static NonNullList<ItemStack> getAllItemsFromNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        NonNullList<ItemStack>[] lists = createLists();

        for (int i = 0; i < KEY.length; i++) {
            ItemUtils.readInventory(provider, nbt, KEY[i], lists[i]);
        }

        NonNullList<ItemStack> result = NonNullList.create();
        for (NonNullList<ItemStack> list : lists) {
            for (ItemStack stack : list) {
                if (!stack.isEmpty()) result.add(stack);
            }
        }

        return result;
    }

    public static void restoreFromNBT(ServerPlayer player, CompoundTag data, boolean restoreAccessories) {
        NonNullList<ItemStack>[] lists = createLists();
        var reg = player.registryAccess();

        for (int i = 0; i < KEY.length; i++) {
            ItemUtils.readInventory(reg, data, KEY[i], lists[i]);
        }

        Inventory inv = player.getInventory();
        restoreInventory(inv.items, lists[0], player);
        restoreInventory(inv.armor, lists[1], player);
        restoreInventory(inv.offhand, lists[2], player);

        if (restoreAccessories && AccessoryLoader.hasAnyHandler() && data.contains(KEYS.getAccessories(), 10)) {
            var accessories = AccessoryLoader.loadNBT(data.getCompound(KEYS.getAccessories()), reg);
            AccessoryLoader.restoreAccessories(player, accessories, true);
        }
    }

    private static NonNullList<ItemStack>[] createLists() {
        return new NonNullList[]{
                NonNullList.withSize(SIZES[0], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[1], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[2], ItemStack.EMPTY)
        };
    }

    public static void restoreInventory(NonNullList<ItemStack> target, NonNullList<ItemStack> source, ServerPlayer player) {
        for (int i = 0; i < Math.min(source.size(), target.size()); i++) {
            ItemStack item = source.get(i);
            if (item.isEmpty()) continue;

            if (target.get(i).isEmpty()) {
                target.set(i, item.copy());
            } else {
                giveOrDropItem(player, item.copy());
            }
        }
    }

    public static void giveOrDropItem(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static final Map<String, Double> VALUABLE_ITEMS = new LinkedHashMap<>() {{
        put("minecraft:saddle", 1.25d);
        put("minecraft:name_tag", 1d);
        put("#c:ingots", 5d);
        put("#c:gems", 8d);
        put("#c:storage_blocks", 16d);
        put("#c:ores", 4d);
        put("#c:raw_materials", 4d);
        put("#c:rods", 6d);
        put("#c:alloys", 6d);
        put("#c:circuits", 8d);
        put("#c:dusts", 1d);
        put("#c:foods/golden", 16d);
        put("#c:tools", 2d);
        put("#c:armors", 2d);
        put("#c:music_discs", 8d);
    }};

    public static GraveStoneLevels calculateGraveLevel(Player player) {
        List<ItemStack> allItems = new ArrayList<>();
        allItems.addAll(player.getInventory().items);
        allItems.addAll(player.getInventory().armor);
        allItems.addAll(player.getInventory().offhand);

        double score = player.experienceLevel;
        for (ItemStack stack : allItems) {
            if (stack.isEmpty()) continue;
            score += getItemScore(stack) * stack.getCount();
        }

        if (score >= 80) return GraveStoneLevels.GRAVESTONE_LEVEL_4;
        if (score >= 50) return GraveStoneLevels.GRAVESTONE_LEVEL_3;
        if (score >= 20) return GraveStoneLevels.GRAVESTONE_LEVEL_2;
        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }

    public static int getXpNeededForLevel(int level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }


    public static int getTotalXpFromLevelAndProgress(int level, float progress) {
        return getTotalXpToReachLevel(level) + (int) (progress * getXpNeededForLevel(level));
    }

    private static int getTotalXpToReachLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    private static double getItemScore(ItemStack stack) {
        for (Map.Entry<String, Double> entry : VALUABLE_ITEMS.entrySet()) {
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

    public static NonNullList<ItemStack> parseArmor(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getArmorInventory(), armor);
        return armor;
    }

    public static NonNullList<ItemStack> parseMainInventory(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> main = NonNullList.withSize(36, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getMainInventory(), main);
        return main;
    }
}