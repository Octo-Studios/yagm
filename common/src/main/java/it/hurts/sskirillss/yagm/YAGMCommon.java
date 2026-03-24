package it.hurts.sskirillss.yagm;

import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import it.hurts.sskirillss.yagm.init.*;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;

@Slf4j
public class YAGMCommon {
    public static final String MODID = "yagm";

    public static void init(){
        BlockRegistry.init();
        EntityRegistry.init();
        ItemsRegistry.init();
        EventRegistry.init();
        CommandRegistry.init();
        ParticleRegistry.init();
        CreativeTabsRegistry.init();
        DefaultVariantsRegistry.registerAll();
        YAGMCompat.init();

    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}