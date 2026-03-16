package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.particles.candle.CandleParticleInit;
import it.hurts.sskirillss.yagm.register.EntityRegistry;

public class YAGMClient {

    public static void init() {
        registerEntityRenderers();
        CandleParticleInit.init();
        EmissiveModelRegistry.init();

    }

    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GRAVE_STONE, GraveStoneEntityRenderer::new);
    }
}
