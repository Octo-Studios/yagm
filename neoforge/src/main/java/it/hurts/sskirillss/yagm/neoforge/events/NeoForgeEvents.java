package it.hurts.sskirillss.yagm.neoforge.events;

import it.hurts.sskirillss.yagm.utils.ItemValuator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@EventBusSubscriber
public class NeoForgeEvents {


    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ItemValuator.initialize(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ItemValuator.shutdown();
    }
}