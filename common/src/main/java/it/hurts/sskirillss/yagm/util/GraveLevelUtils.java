package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GraveLevelUtils {

    public static final Map<String, Double> VALUABLE_ITEMS = new LinkedHashMap<>() {{
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
            score = score + InventoryUtils.getItemScore(stack) * stack.getCount();
        }

        if (score >= 80) return GraveStoneLevels.GRAVESTONE_LEVEL_4;
        if (score >= 50) return GraveStoneLevels.GRAVESTONE_LEVEL_3;
        if (score >= 20) return GraveStoneLevels.GRAVESTONE_LEVEL_2;
        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }
}
