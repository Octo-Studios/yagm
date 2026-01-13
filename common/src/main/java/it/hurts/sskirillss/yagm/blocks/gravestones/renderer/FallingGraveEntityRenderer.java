package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.FallingGraveEntity;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FallingGraveEntityRenderer extends EntityRenderer<FallingGraveEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public FallingGraveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(FallingGraveEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        poseStack.translate(-0.5, 0, -0.5);

        poseStack.translate(0.5, 0.5, 0.5);
        float rotation = entity.getGraveRotation(partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(-0.5, -0.5, -0.5);
        
        GraveStoneLevels level = entity.getGraveLevel();
        IGraveVariant variant = entity.getVariant();
        IGraveVariant defaultVariant = GraveVariantRegistry.getDefaultVariant();
        
        // Check if we have a non-default variant
        boolean useVariantModel = variant != null && variant != defaultVariant;
        
        if (useVariantModel) {
            // Render the variant model
            ResourceLocation modelLocation = variant.getModel(level.getLevel());
            ModelResourceLocation modelResLoc = new ModelResourceLocation(modelLocation, "standalone");
            
            BakedModel model = blockRenderer.getBlockModelShaper().getModelManager().getModel(modelResLoc);
            BakedModel missingModel = blockRenderer.getBlockModelShaper().getModelManager().getMissingModel();
            
            if (model != missingModel) {
                BlockState state = BlockRegistry.getBlockForLevel(level).defaultBlockState();
                VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
                blockRenderer.getModelRenderer().renderModel(poseStack.last(), vertexConsumer, state, model, 1.0f, 1.0f, 1.0f, packedLight, OverlayTexture.NO_OVERLAY);
            } else {
                // Fallback to default block if model is missing
                renderDefaultBlock(level, poseStack, buffer, packedLight);
            }
        } else {
            // Render the default block
            renderDefaultBlock(level, poseStack, buffer, packedLight);
        }

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }
    
    private void renderDefaultBlock(GraveStoneLevels level, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Block block = BlockRegistry.getBlockForLevel(level);
        BlockState state = block.defaultBlockState();
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(FallingGraveEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}