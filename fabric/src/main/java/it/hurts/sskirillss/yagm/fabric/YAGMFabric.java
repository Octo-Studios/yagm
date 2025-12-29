package it.hurts.sskirillss.yagm.fabric;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.utils.ItemValuator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class YAGMFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YAGMCommon.init();
        System.out.println("[YAGM] ========== YAGM FABRIC INIT ==========");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            System.out.println("[YAGM] SERVER STARTED - initializing ItemValuator");
            ItemValuator.initialize(server);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ItemValuator.shutdown());
    }

}