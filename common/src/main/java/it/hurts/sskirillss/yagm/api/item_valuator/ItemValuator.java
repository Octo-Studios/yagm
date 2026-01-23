package it.hurts.sskirillss.yagm.api.item_valuator;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.item_valuator.abstract_validation.AbstractItemValuator;
import it.hurts.sskirillss.yagm.api.item_valuator.config.ValuatorConfig;
import it.hurts.sskirillss.yagm.api.item_valuator.provider.ValueProviderRegistry;
import it.hurts.sskirillss.yagm.api.item_valuator.providers.ILevelDeterminer;
import it.hurts.sskirillss.yagm.api.item_valuator.providers.IValueModifier;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

@SuppressWarnings("all")
public class ItemValuator extends AbstractItemValuator {

    private static ItemValuator INSTANCE;

    private static final double[] LIST_THRESHOLDS = {0, 100, 500, 4000};
    private static final double DEFAULT_VALUE = 1.0;
    private static final double DEFAULT_RARITY_MULTIPLIER = 0.1;

    private final GraveLevelDeterminer levelDeterminer;

    private ItemValuator(MinecraftServer server, ValuatorConfig config) {
        super(server, config);
        this.levelDeterminer = new GraveLevelDeterminer();
    }

    public static void initialize(MinecraftServer server) {
        Path configDir = server.getWorldPath(LevelResource.ROOT).resolve("config");
        ValuatorConfig config = new ValuatorConfig(configDir, YAGMCommon.MODID);
        config.setLevelThresholds(LIST_THRESHOLDS);
        config.setDefaultValue(DEFAULT_VALUE);
        config.setRarityMultiplier(DEFAULT_RARITY_MULTIPLIER);
        config.setExportValues(true);

        INSTANCE = new ItemValuator(server, config);
        INSTANCE.registerDefaultModifiers();
        INSTANCE.initialize();
        YAGMCommon.LOGGER.info("ItemValuator initialized (base cache size: {}), recipe values computing in background.", INSTANCE.valueCache.size());
    }

    public static void shutdown() {
        INSTANCE = null;
    }

    public static boolean isAvailable() {
        return INSTANCE != null;
    }

    public static ItemValuator getInstance() {
        return INSTANCE;
    }

    private void registerDefaultModifiers() {
        ValueProviderRegistry.registerModifier(new IValueModifier() {
            private final TagKey<Item> INGOTS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots"));

            @Override
            public double modify(ItemStack stack, double value) {
                return stack.is(INGOTS) ? Math.max(value, 10.0) : value;
            }

            @Override
            public String getId() {
                return "ingot_modifier";
            }

            @Override
            public int getPriority() {
                return 100;
            }
        });
    }

    public GraveStoneLevels determineGraveLevel(Player player) {
        double value = calculateInventoryValue(player);
        return determineLevelByValue(value);
    }

    public GraveStoneLevels determineGraveLevel(NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        double value = calculateValue(main, armor, offhand);
        return determineLevelByValue(value);
    }

    public GraveStoneLevels determineLevelByValue(double value) {
        return determineLevel(value, levelDeterminer);
    }


    private static class GraveLevelDeterminer implements ILevelDeterminer<GraveStoneLevels> {

        @Override
        public GraveStoneLevels determine(double value) {
            GraveStoneLevels[] levels = getAllLevels();

            for (int i = levels.length - 1; i >= 0; i--) {
                if (value >= getThreshold(levels[i])) {
                    return levels[i];
                }
            }

            return getDefault();
        }

        @Override
        public GraveStoneLevels[] getAllLevels() {
            return GraveStoneLevels.values();
        }

        @Override
        public double getThreshold(GraveStoneLevels level) {
            return switch (level) {
                case GRAVESTONE_LEVEL_1 -> 0;
                case GRAVESTONE_LEVEL_2 -> 100;
                case GRAVESTONE_LEVEL_3 -> 500;
                case GRAVESTONE_LEVEL_4 -> 4000;
            };
        }

        @Override
        public GraveStoneLevels getDefault() {
            return GraveStoneLevels.GRAVESTONE_LEVEL_1;
        }
    }
}
