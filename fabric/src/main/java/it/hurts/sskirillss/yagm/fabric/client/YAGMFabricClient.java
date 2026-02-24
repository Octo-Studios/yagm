package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.client.particles.type.CandleFlameParticle;
import it.hurts.sskirillss.yagm.client.particles.type.Level4GraveParticle;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import it.hurts.sskirillss.yagm.register.ParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;


public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        BlockEntityRendererRegistry.register(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
    }

    public static void registerParticleFactories() {
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.LEVEL4_GRAVE.get(), Level4GraveParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.SOUL_CANDLE_FLAME.get(), CandleFlameParticle.Provider::new);
    }


    @Override
    public void onInitializeClient() {
        YAGMClient.init();
        registerEntityRenderers();
        registerParticleFactories();

    }
}
