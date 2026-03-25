package it.hurts.sskirillss.yagm.neoforge.client;

import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.particle.GroundDustParticle;
import it.hurts.sskirillss.yagm.client.particle.GraveTrailParticle;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;


@EventBusSubscriber(modid = YAGMCommon.MODID, value = Dist.CLIENT)
public class YAGMNeoForgeClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(YAGMClient::init);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.GRAVE_DUST_FLAT.get(), GroundDustParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.GRAVE_TRAIL.get(), GraveTrailParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.FALLING_GRAVE.get(), FallingGraveEntityRenderer::new);
        event.registerEntityRenderer(EntityRegistry.GRAVE_STONE.get(), GraveStoneEntityRenderer::new);
    }
    
}
