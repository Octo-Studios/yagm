package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import it.hurts.octostudios.octolib.module.particle.trail.ParticleTrailRegistry;
import it.hurts.sskirillss.yagm.client.particle.FireParticle;
import it.hurts.sskirillss.yagm.client.particle.GroundDustParticle;
import it.hurts.sskirillss.yagm.client.particle.GraveTrailParticle;
import it.hurts.sskirillss.yagm.client.particle.type.GraveDustParticleTrail;
import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GhostEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.init.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;

public class YAGMClient {

    public static void init() {
        registerEntityRenderers();
        registerParticleProviders();
        registerTrailProviders();
    }

    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GHOST, GhostEntityRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
    }

    private static void registerParticleProviders() {
        ParticleProviderRegistry.register(ParticleRegistry.GRAVE_DUST_FLAT.get(), GroundDustParticle.Provider::new);
        ParticleProviderRegistry.register(ParticleRegistry.GRAVE_TRAIL.get(), GraveTrailParticle.Provider::new);
        ParticleProviderRegistry.register(ParticleRegistry.CANDLE_FLAME.get(), FireParticle.Provider::new);
    }

    private static void registerTrailProviders() {
        ParticleTrailRegistry.registerProvider(ParticleRegistry.GRAVE_TRAIL.get(), GraveDustParticleTrail::new);
    }
}
