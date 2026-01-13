package it.hurts.sskirillss.yagm.neoforge.client;


import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantModelRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.blocks.gravestones.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.client.YAGMClient;
import it.hurts.sskirillss.yagm.register.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.EntityRegistry;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;


@EventBusSubscriber(value = Dist.CLIENT)
public class YAGMNeoForgeClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(YAGMClient::init);
    }

    @SubscribeEvent
    public static void onRegisterAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ResourceLocation model : GraveVariantModelRegistry.getVariantModels()) {
            event.register(ModelResourceLocation.inventory(model));
        }
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.FALLING_GRAVE.get(), FallingGraveEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
    }
}
