package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GraveLevelUtils {

    public static GraveStoneLevels calculateGraveLevel(Player player) {
        var config = ConfigRegistry.CONFIG;

        return switch (config.getTierMode()) {
            case FIXED -> fromTier(config.getFixedTier());
            case RANDOM -> fromTier(config.getRandomTierMin() + player.getRandom().nextInt(config.getRandomTierMax() - config.getRandomTierMin() + 1));
            case AUTO -> calculateGraveLevelByScore(player);
        };
    }

    private static GraveStoneLevels fromTier(int tier) {
        return GraveStoneLevels.values()[Math.clamp(tier, 1, GraveStoneLevels.values().length) - 1];
    }

    private static GraveStoneLevels calculateGraveLevelByScore(Player player) {
        var config = ConfigRegistry.CONFIG;

        List<ItemStack> allItems = new ArrayList<>();
        allItems.addAll(player.getInventory().items);
        allItems.addAll(player.getInventory().armor);
        allItems.addAll(player.getInventory().offhand);

        double score = player.experienceLevel * config.getExperienceLevelWeight();
        for (ItemStack stack : allItems) {
            if (stack.isEmpty()) continue;
            score = score + InventoryUtils.getItemScore(stack) * stack.getCount();
        }

        var thresholds = config.getTierThresholds();

        for (var index = Math.min(thresholds.size(), GraveStoneLevels.values().length - 1); index > 0; index--) {
            if (score >= thresholds.get(index - 1)) return fromTier(index + 1);
        }

        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }
}
