package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.titles.renderer.GraveTitleEntityRenderer;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import net.minecraft.client.renderer.RenderType;


public class YAGMClient {

    public static void init() {
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
        BlockEntityRendererRegistry.register(BlockRegistry.GRAVE_STONE_BE.get(), GraveStoneBlockEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GRAVE_TITLE, GraveTitleEntityRenderer::new);
    }
}

