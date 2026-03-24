package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.EmissiveModelRegistry;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.client.particle.type.CandleFlameParticle;
import it.hurts.sskirillss.yagm.client.particle.type.Level4GraveParticle;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;


public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GRAVE_STONE, GraveStoneEntityRenderer::new);
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
        registerEmissiveModels();

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath("yagm", "emissive_models");
                    }
                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        Minecraft.getInstance().execute(EmissiveModelRegistry::onResourcesReloaded);
                    }
                });
    }

    private static void registerEmissiveModels() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.addModels(EmissiveModelRegistry.getModelIds());
        });
    }
}
