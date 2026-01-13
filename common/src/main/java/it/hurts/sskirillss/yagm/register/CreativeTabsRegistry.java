package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;


public class CreativeTabsRegistry {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(YAGMCommon.MODID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> YAGM = TABS.register(
            "yagm",
            () -> CreativeTabRegistry.create(
                    Component.translatable("categorie.yagm.main"),
                    () -> new ItemStack(ItemsRegistry.GRAVESTONE_TIER_3.get())
            )
    );

    public static void init() {
        TABS.register();
    }
}