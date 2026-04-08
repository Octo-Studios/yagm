package it.hurts.sskirillss.yagm;

import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import it.hurts.sskirillss.yagm.init.*;
import net.minecraft.resources.ResourceLocation;

import java.util.logging.Logger;

public class YAGMCommon {
    public static final String MODID = "yagm";
    public static final Logger LOGGER = Logger.getLogger(MODID);

    public static void init(){
        BlockRegistry.init();
        BlockEntityRegistry.init();
        EntityRegistry.init();
        ItemsRegistry.init();
        EventRegistry.init();
        CommandRegistry.init();
        SoundRegistry.init();
        ParticleRegistry.init();
        CreativeTabsRegistry.init();
        DefaultVariantsRegistry.registerAll();
        YAGMCompat.init();

    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}