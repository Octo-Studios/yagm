package it.hurts.sskirillss.yagm.neoforge.client;


import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.client.particles.type.CandleFlameParticle;
import it.hurts.sskirillss.yagm.client.particles.type.Level4GraveParticle;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import it.hurts.sskirillss.yagm.register.ParticleRegistry;
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
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.FALLING_GRAVE.get(), FallingGraveEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.LEVEL4_GRAVE.get(), Level4GraveParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.SOUL_CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
    }
}
