package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;

public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GRAVE_STONE, GraveStoneEntityRenderer::new);
    }

    @Override
    public void onInitializeClient() {
        YAGMClient.init();
        registerEntityRenderers();

    }
}
