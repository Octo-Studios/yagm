package it.hurts.sskirillss.yagm;

import dev.architectury.event.events.common.LifecycleEvent;
import it.hurts.sskirillss.yagm.register.*;
import it.hurts.sskirillss.yagm.utils.ItemValuator;
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
        CreativeTabsRegistry.init();
        LifecycleEvent.SERVER_STARTED.register(ItemValuator::initialize);
        LifecycleEvent.SERVER_STOPPING.register(server -> ItemValuator.shutdown());
    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}