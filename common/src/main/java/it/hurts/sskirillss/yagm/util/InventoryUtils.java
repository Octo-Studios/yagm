package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;


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

        nbt.putLong(KEYS.getTotalExperience(), getTotalXpFromLevelAndProgress(player.experienceLevel, player.experienceProgress));

        nbt.putDouble(KEYS.getDeathPosX(), player.getX());
        nbt.putDouble(KEYS.getDeathPosY(), player.getY());
        nbt.putDouble(KEYS.getDeathPosZ(), player.getZ());
        nbt.putString(KEYS.getDimension(), player.level().dimension().location().toString());

        return nbt;
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

        return AccessoryLoader.hasAnyHandler() && !AccessoryLoader.collectAccessories(player).isEmpty();
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

    public static void restoreFromNBT(ServerPlayer player, CompoundTag data, boolean restoreAccessories, BlockPos dropPos) {
        NonNullList<ItemStack>[] lists = createLists();
        var reg = player.registryAccess();

        for (int i = 0; i < KEY.length; i++) {
            ItemUtils.readInventory(reg, data, KEY[i], lists[i]);
        }

        Inventory inv = player.getInventory();
        restoreInventory(inv.items, lists[0], player, dropPos);
        restoreInventory(inv.armor, lists[1], player, dropPos);
        restoreInventory(inv.offhand, lists[2], player, dropPos);

        if (restoreAccessories && AccessoryLoader.hasAnyHandler() && data.contains(KEYS.getAccessories(), 10)) {
            var accessories = AccessoryLoader.loadNBT(data.getCompound(KEYS.getAccessories()), reg);
            AccessoryLoader.restoreAccessories(player, accessories, true, dropPos, player.level());
        }
    }

    private static NonNullList<ItemStack>[] createLists() {
        return new NonNullList[]{
                NonNullList.withSize(SIZES[0], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[1], ItemStack.EMPTY),
                NonNullList.withSize(SIZES[2], ItemStack.EMPTY)
        };
    }

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

    public static void giveOrDropItem(ServerPlayer player, ItemStack stack, BlockPos dropPos) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            if (dropPos != null) {
                Containers.dropItemStack(player.level(), dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, stack);
            } else {
                player.drop(stack, false);
            }
        }
    }

    private static final Map<String, Double> VALUABLE_ITEMS = new LinkedHashMap<>() {{
        put("#c:ingots", 1.5d);
        put("#c:gems", 2.4d);
        put("#c:storage_blocks", 4.8d);
        put("#c:ores", 1.2d);
        put("#c:raw_materials", 1.2d);
        put("#c:rods", 1.8d);
        put("#c:alloys", 1.8d);
        put("#c:circuits", 2.4d);
        put("#c:dusts", 0.3d);
        put("#c:foods/golden", 4.8d);
        put("#c:tools", 0.6d);
        put("#c:armors", 0.6d);
        put("#c:blocks", 0.1d);
        put("#c:music_discs", 2.4d);
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

    public static void restoreFullGrave(ServerPlayer player, CompoundTag data) {
        restoreFullGrave(player, data, null);
    }

    public static void restoreFullGrave(ServerPlayer player, CompoundTag data, BlockPos dropPos) {
        restoreFromNBT(player, data, true, dropPos);

        long xp = data.getLong(KEYS.getTotalExperience());
        if (xp > 0) {
            addExperience(player, xp);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static void dropFullGrave(Level level, BlockPos pos, CompoundTag data) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        for (ItemStack item : getAllItemsFromNBT(level.registryAccess(), data)) {
            if (!item.isEmpty()) {
                Containers.dropItemStack(level, x, y, z, item);
            }
        }

        if (AccessoryLoader.hasAnyHandler() && data.contains(KEYS.getAccessories(), Tag.TAG_COMPOUND)) {
            AccessoryLoader.loadNBT(data.getCompound(KEYS.getAccessories()), level.registryAccess()).values()
                    .forEach(slots -> slots.values().forEach(item -> {
                        if (!item.isEmpty()) {
                            Containers.dropItemStack(level, x, y, z, item);
                        }
                    }));
        }

        if (level instanceof ServerLevel serverLevel) {
            long xp = data.getLong(KEYS.getTotalExperience());
            if (xp > 0) {
                ExperienceOrb.award(serverLevel, new Vec3(x, y, z), (int) Math.min(Integer.MAX_VALUE, xp));
            }
        }
    }

    public static long getXpForLevel(long level) {
        if (level < 0) return 0;
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    public static long getTotalXpFromLevelAndProgress(long level, double progress) {
        return getTotalXpToReachLevel(level) + (long) (progress * getXpForLevel(level));
    }

    private static long getTotalXpToReachLevel(long level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (5 * level * level - 81 * level + 720) / 2;
        return (9 * level * level - 325 * level + 4440) / 2;
    }

    public static void clearExperience(ServerPlayer player) {
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
    }

    private static void addExperience(ServerPlayer player, long xpToAdd) {
        long current = getTotalXpFromLevelAndProgress(player.experienceLevel, player.experienceProgress);
        long total = Long.MAX_VALUE - current < xpToAdd ? Long.MAX_VALUE : current + xpToAdd;
        applyTotalExperience(player, total);
    }

    private static void applyTotalExperience(ServerPlayer player, long totalXp) {
        XpState state = decodeTotalExperience(totalXp);
        player.experienceLevel = state.level();
        player.experienceProgress = state.progress();
        player.totalExperience = (int) Math.min(Integer.MAX_VALUE, totalXp);
    }

    private static XpState decodeTotalExperience(long totalXp) {
        if (totalXp <= 0) {
            return new XpState(0, 0.0f);
        }

        long low = 0;
        long high = 1;

        while (high < Integer.MAX_VALUE && getTotalXpToReachLevel(high) <= totalXp) {
            long next = high << 1;
            high = next <= 0 || next > Integer.MAX_VALUE ? Integer.MAX_VALUE : next;
        }

        while (low < high) {
            long mid = (low + high + 1) >>> 1;
            if (getTotalXpToReachLevel(mid) <= totalXp) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }

        long level = Math.min(low, Integer.MAX_VALUE);
        long base = getTotalXpToReachLevel(level);
        long toNext = Math.max(1L, getXpForLevel(level));
        float progress = (float) Math.min(0.999999, Math.max(0.0, (double) (totalXp - base) / (double) toNext));

        return new XpState((int) level, progress);
    }

    private record XpState(int level, float progress) {
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

    public static NonNullList<ItemStack> parseOffHand(RegistryAccess registry, CompoundTag data) {
        NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
        ItemUtils.readInventory(registry, data, KEYS.getOffhandInventory(), offhand);
        return offhand;
    }
}
