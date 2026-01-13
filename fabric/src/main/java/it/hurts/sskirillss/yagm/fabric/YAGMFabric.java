package it.hurts.sskirillss.yagm.fabric;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import it.hurts.sskirillss.yagm.api.item_valuator.ItemValuator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class YAGMFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YAGMCommon.init();
        YAGMCompat.init();

        ServerLifecycleEvents.SERVER_STARTED.register(ItemValuator::initialize);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ItemValuator.shutdown());
    }
}