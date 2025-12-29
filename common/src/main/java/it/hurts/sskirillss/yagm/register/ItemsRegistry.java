package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

import static it.hurts.sskirillss.yagm.YAGMCommon.id;


@SuppressWarnings("all")
public final class ItemsRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(YAGMCommon.MODID, Registries.ITEM);


    public static RegistrySupplier<Item> item(String name) {
        return item(name, new Item.Properties(), Item::new);
    }

    public static <I extends Item> RegistrySupplier<I> item(String name, Function<Item.Properties, I> itemFunc) {
        return item(name, new Item.Properties(), itemFunc);
    }

    public static <I extends Item> @NotNull RegistrySupplier<I> item(String name, Item.Properties properties, Function<Item.Properties, I> itemFunc) {
        ResourceLocation id = id(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties propsWithId = properties.arch$tab(CreativeTabsRegistry.YAGM);
        return ITEMS.register(id, () -> itemFunc.apply(propsWithId));
    }

    public static <B extends Block> RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<B> block) {
        return blockItem(name, block, new Item.Properties());
    }

    public static <B extends Block> RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<B> block, Item.Properties properties) {
        ResourceLocation id = id(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties propsWithId = properties.arch$tab(CreativeTabsRegistry.YAGM);
        return ITEMS.register(id, () -> new BlockItem(block.get(), propsWithId));
    }


    public static final RegistrySupplier<BlockItem> OLD_WOODEN_CROSS = blockItem("old_wooden_cross", BlockRegistry.GRAVESTONE_LEVEL_1);
    public static final RegistrySupplier<BlockItem> WOODEN_CROSS = blockItem("wooden_cross", BlockRegistry.GRAVESTONE_LEVEL_2);
    public static final RegistrySupplier<BlockItem> STONE_TOMBSTONE = blockItem("stone_tombstone", BlockRegistry.GRAVESTONE_LEVEL_3);
    public static final RegistrySupplier<BlockItem> INLAID_STONE_TOMBSTONE = blockItem("inlaid_stone_tombstone", BlockRegistry.GRAVESTONE_LEVEL_4);
    public static final RegistrySupplier<BlockItem> GRANITE_TOMBSTONE = blockItem("granite_tombstone", BlockRegistry.GRAVESTONE_LEVEL_5);

    public static void init() {
         ITEMS.register();
    }
}