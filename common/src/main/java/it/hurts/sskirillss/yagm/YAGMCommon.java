package it.hurts.sskirillss.yagm;

import dev.architectury.event.events.common.LifecycleEvent;
import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import it.hurts.sskirillss.yagm.api.item_valuator.ItemValuator;
import it.hurts.sskirillss.yagm.register.*;
import it.hurts.sskirillss.yagm.test.CemeteryTestLogger;
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
        DefaultVariantsRegistry.registerAll();
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ItemValuator.initialize(server);
            CemeteryTestLogger.init(server);
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> ItemValuator.shutdown());
        YAGMCompat.init();

    }

    public static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, name);
    }
}