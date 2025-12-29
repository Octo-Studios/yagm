package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.DeferredSupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

import static it.hurts.sskirillss.yagm.YAGMCommon.id;


public class CreativeTabsRegistry {


    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(YAGMCommon.MODID, Registries.CREATIVE_MODE_TAB);

    public static final DeferredSupplier<CreativeModeTab> YAGM = create("yagm");

    @SuppressWarnings("all")
    public static DeferredSupplier<CreativeModeTab> create(String name) {
        return CreativeTabRegistry.defer(id(name));
    }

    public static void register() {
        registerTab("yagm", ItemsRegistry.GRANITE_TOMBSTONE);
        registerTab("yagm", ItemsRegistry.INLAID_STONE_TOMBSTONE);
        registerTab("yagm", ItemsRegistry.WOODEN_CROSS);
        registerTab("yagm", ItemsRegistry.STONE_TOMBSTONE);
        registerTab("yagm", ItemsRegistry.OLD_WOODEN_CROSS);
        CREATIVE_MODE_TABS.register();
    }

    public static void registerTab(String name, Supplier<? extends ItemLike> icon) {
        CREATIVE_MODE_TABS.register(name,
                () -> CreativeTabRegistry.create(
                        Component.translatable("itemGroup.yagm." + name),
                        () -> new ItemStack(icon.get())
                ));
    }

}
