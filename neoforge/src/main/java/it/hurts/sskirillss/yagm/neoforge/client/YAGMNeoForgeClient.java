package it.hurts.sskirillss.yagm.neoforge.client;


import it.hurts.sskirillss.yagm.client.YAGMClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;


@EventBusSubscriber(value = Dist.CLIENT)
public class YAGMNeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(YAGMClient::init);
    }
}
