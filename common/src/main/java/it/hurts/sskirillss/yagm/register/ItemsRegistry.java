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
        Item.Properties propsWithId = properties.arch$tab(CreativeTabsRegistry.YAGM_TAB);
        return ITEMS.register(id, () -> itemFunc.apply(propsWithId));
    }

    public static <B extends Block> RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<B> block) {
        return blockItem(name, block, new Item.Properties());
    }

    public static <B extends Block> RegistrySupplier<BlockItem> blockItem(String name, RegistrySupplier<B> block, Item.Properties properties) {
        ResourceLocation id = id(name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        Item.Properties propsWithId = properties.arch$tab(CreativeTabsRegistry.YAGM_TAB);
        return ITEMS.register(id, () -> new BlockItem(block.get(), propsWithId));
    }


    public static final RegistrySupplier<BlockItem> GRAVE_STONE_ITEM = blockItem("grave_stone", BlockRegistry.GRAVE_STONE);

    public static void init() {
         ITEMS.register();
    }
}