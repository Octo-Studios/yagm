package it.hurts.sskirillss.yagm;

import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import it.hurts.sskirillss.yagm.register.EventRegistry;
import it.hurts.sskirillss.yagm.register.ItemsRegistry;
import it.hurts.sskirillss.yagm.register.CreativeTabsRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YAGMCommon {
    public static final String MODID = "yagm";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public static void init(){
        CreativeTabsRegistry.init();
        BlockRegistry.init();
        EntityRegistry.init();
        ItemsRegistry.init();
        EventRegistry.init();
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}