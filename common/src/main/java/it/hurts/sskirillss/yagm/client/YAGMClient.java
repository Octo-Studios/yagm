package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import net.minecraft.client.renderer.RenderType;


public class YAGMClient {

    public static void init() {
        registerEntityRenderers();
        registerRenderTypes();
        registerBlockEntityRenderers();
    }

    private static void registerRenderTypes() {
        RenderTypeRegistry.register(RenderType.cutout(),
                BlockRegistry.GRAVESTONE_LEVEL_1.get(),
                BlockRegistry.GRAVESTONE_LEVEL_2.get(),
                BlockRegistry.GRAVESTONE_LEVEL_3.get(),
                BlockRegistry.GRAVESTONE_LEVEL_4.get()
        );
    }

    private static void registerBlockEntityRenderers() {
        BlockEntityRendererRegistry.register(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
    }

    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
    }
}

