package it.hurts.sskirillss.yagm.neoforge.client;


import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.EmissiveModelRegistry;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.client.particle.type.CandleFlameParticle;
import it.hurts.sskirillss.yagm.client.particle.type.Level4GraveParticle;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
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
        event.registerEntityRenderer(EntityRegistry.GRAVE_STONE.get(), GraveStoneEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.LEVEL4_GRAVE.get(), Level4GraveParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.SOUL_CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        EmissiveModelRegistry.init();
        for (ResourceLocation modelId : EmissiveModelRegistry.getModelIds()) {
            event.register(new ModelResourceLocation(modelId, "standalone"));
        }
    }

    @SubscribeEvent
    public static void onModelsBaked(ModelEvent.BakingCompleted event) {
        EmissiveModelRegistry.onResourcesReloaded(event.getModelManager());
    }

}
