package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.FallingGraveEntity;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
        ResourceLocation variantId = entity.getVariantId();
        String variantStr = variantId != null ? variantId.toString() : null;
        
        Block block = BlockRegistry.getBlockForVariant(variantStr, level);
        BlockState state = block.defaultBlockState();
        blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(FallingGraveEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}