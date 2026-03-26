package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.model.GhostEntityModel;
import it.hurts.sskirillss.yagm.client.renderer.GhostEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.particle.GroundDustParticle;
import it.hurts.sskirillss.yagm.client.particle.GraveTrailParticle;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;

public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GRAVE_STONE, GraveStoneEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GHOST, GhostEntityRenderer::new);
    }

    public static void registerModelLayers() {
        EntityModelLayerRegistry.registerModelLayer(GhostEntityModel.LAYER_LOCATION, GhostEntityModel::createBodyLayer);
    }

    public static void registerParticles() {
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.GRAVE_DUST_FLAT.get(), GroundDustParticle.Provider::new);
        ParticleFactoryRegistry.getInstance().register(ParticleRegistry.GRAVE_TRAIL.get(), GraveTrailParticle.Provider::new);
    }

    @Override
    public void onInitializeClient() {
        registerModelLayers();
        YAGMClient.init();
        registerEntityRenderers();
        registerParticles();
    }
}
