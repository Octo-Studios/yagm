package it.hurts.sskirillss.yagm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.component.type.GraveStoneLevels;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
        ResourceLocation variantId = entity.getVariantId();
        String variantStr = variantId != null ? variantId.toString() : null;

        Block block = BlockRegistry.getBlockForVariant(variantStr, level);
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, entity.getFacing());
        }
        BakedModel model = blockRenderer.getBlockModel(state);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),
                state,
                model,
                1.0F, 1.0F, 1.0F,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public boolean shouldRender(FallingGraveEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(FallingGraveEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
