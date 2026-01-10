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

    public static final RegistrySupplier<BlockItem> WOODEN_CROSS = blockItem("wooden_cross", BlockRegistry.GRAVESTONE_LEVEL_1);

    public static final RegistrySupplier<BlockItem> STONE_TOMBSTONE = blockItem("stone_tombstone", BlockRegistry.GRAVESTONE_LEVEL_2);

    public static final RegistrySupplier<BlockItem> INLAID_STONE_TOMBSTONE = blockItem("inlaid_stone_tombstone", BlockRegistry.GRAVESTONE_LEVEL_3);

    public static final RegistrySupplier<BlockItem> GRANITE_TOMBSTONE = blockItem("granite_tombstone", BlockRegistry.GRAVESTONE_LEVEL_4);

    public static void init() {
        ITEMS.register();
    }
}