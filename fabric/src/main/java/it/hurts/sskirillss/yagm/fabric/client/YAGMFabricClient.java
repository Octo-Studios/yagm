package it.hurts.sskirillss.yagm.fabric.client;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.world.level.block.entity.BlockEntityType;


public class YAGMFabricClient implements ClientModInitializer {

    public static void registerEntityRenderers(){
        BlockEntityRendererRegistry.register(
            (BlockEntityType<GraveStoneBlockEntity>) BlockEntityRegistry.GRAVE_STONE.get(),
            GraveStoneBlockEntityRenderer::new
        );
    }

    @Override
    public void onInitializeClient() {
        YAGMClient.init();
        registerEntityRenderers();
    }
}
