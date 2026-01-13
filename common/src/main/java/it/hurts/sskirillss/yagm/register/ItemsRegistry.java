package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("all")
public final class ItemsRegistry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(YAGMCommon.MODID, Registries.ITEM);

    public static <B extends Block> RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<B> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties().arch$tab(CreativeTabsRegistry.YAGM)));
    }

    public static final RegistrySupplier<BlockItem> GRAVESTONE_TIER_1 = blockItem("grave_tier_1", BlockRegistry.GRAVESTONE_LEVEL_1);

    public static final RegistrySupplier<BlockItem> GRAVESTONE_TIER_2 = blockItem("grave_tier_2", BlockRegistry.GRAVESTONE_LEVEL_2);

    public static final RegistrySupplier<BlockItem> GRAVESTONE_TIER_3 = blockItem("grave_tier_3", BlockRegistry.GRAVESTONE_LEVEL_3);

    public static final RegistrySupplier<BlockItem> GRAVESTONE_TIER_4 = blockItem("grave_tier_4", BlockRegistry.GRAVESTONE_LEVEL_4);

    public static final RegistrySupplier<BlockItem> COLD_GRAVESTONE_TIER_1 = blockItem("cold_grave_tier_1", BlockRegistry.COLD_GRAVESTONE_1);

    public static final RegistrySupplier<BlockItem> COLD_GRAVESTONE_TIER_2 = blockItem("cold_grave_tier_2", BlockRegistry.COLD_GRAVESTONE_2);

    public static final RegistrySupplier<BlockItem> COLD_GRAVESTONE_TIER_3 = blockItem("cold_grave_tier_3", BlockRegistry.COLD_GRAVESTONE_3);

    public static final RegistrySupplier<BlockItem> COLD_GRAVESTONE_TIER_4 = blockItem("cold_grave_tier_4", BlockRegistry.COLD_GRAVESTONE_4);

    public static final RegistrySupplier<BlockItem> HOT_GRAVESTONE_TIER_1 = blockItem("hot_grave_tier_1", BlockRegistry.HOT_GRAVESTONE_1);

    public static final RegistrySupplier<BlockItem> HOT_GRAVESTONE_TIER_2 = blockItem("hot_grave_tier_2", BlockRegistry.HOT_GRAVESTONE_2);

    public static final RegistrySupplier<BlockItem> HOT_GRAVESTONE_TIER_3 = blockItem("hot_grave_tier_3", BlockRegistry.HOT_GRAVESTONE_3);

    public static final RegistrySupplier<BlockItem> HOT_GRAVESTONE_TIER_4 = blockItem("hot_grave_tier_4", BlockRegistry.HOT_GRAVESTONE_4);

    public static final RegistrySupplier<BlockItem> NETHER_GRAVESTONE_TIER_1 = blockItem("nether_grave_tier_1", BlockRegistry.NETHER_GRAVESTONE_1);

    public static final RegistrySupplier<BlockItem> NETHER_GRAVESTONE_TIER_2 = blockItem("nether_grave_tier_2", BlockRegistry.NETHER_GRAVESTONE_2);

    public static final RegistrySupplier<BlockItem> NETHER_GRAVESTONE_TIER_3 = blockItem("nether_grave_tier_3", BlockRegistry.NETHER_GRAVESTONE_3);

    public static final RegistrySupplier<BlockItem> NETHER_GRAVESTONE_TIER_4 = blockItem("nether_grave_tier_4", BlockRegistry.NETHER_GRAVESTONE_4);

    public static final RegistrySupplier<BlockItem> TROPICS_GRAVESTONE_TIER_1 = blockItem("tropics_grave_tier_1", BlockRegistry.TROPICS_GRAVESTONE_1);

    public static final RegistrySupplier<BlockItem> TROPICS_GRAVESTONE_TIER_2 = blockItem("tropics_grave_tier_2", BlockRegistry.TROPICS_GRAVESTONE_2);


    public static void init() {
        ITEMS.register();
    }
}