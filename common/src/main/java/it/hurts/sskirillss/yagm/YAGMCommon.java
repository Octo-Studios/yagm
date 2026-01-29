package it.hurts.sskirillss.yagm;

import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import it.hurts.sskirillss.yagm.register.*;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YAGMCommon {
    public static final String MODID = "yagm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init(){
        BlockRegistry.init();
        BlockEntityRegistry.init();
        EntityRegistry.init();
        ItemsRegistry.init();
        EventRegistry.init();
        CommandRegistry.init();
        CommandRegistry.init();
        CreativeTabsRegistry.init();
        DefaultVariantsRegistry.registerAll();
        YAGMCompat.init();

    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}