package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static it.hurts.sskirillss.yagm.YAGMCommon.MODID;

public class CreativeTabsRegistry {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MODID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> YAGM_TAB = TABS.register("yamg", () -> CreativeTabRegistry.create(Component.translatable("categorie.yagm.main"),
            () -> new ItemStack(
                    ItemsRegistry.GRAVE_STONE_ITEM.get()

            )));


    public static void init() {
        TABS.register();
    }
}