package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantModelRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;


public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        BlockEntityRendererRegistry.register(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
    }

    @Override
    public void onInitializeClient() {
        YAGMClient.init();
        registerEntityRenderers();
        ModelLoadingPlugin.register(pluginContext -> {
            for (ResourceLocation model : GraveVariantModelRegistry.getVariantModels()) {
                pluginContext.addModels(model);
            }
        });
    }
}
