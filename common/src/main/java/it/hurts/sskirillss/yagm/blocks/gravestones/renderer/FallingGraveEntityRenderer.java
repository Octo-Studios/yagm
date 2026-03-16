package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.FallingGraveEntity;
import it.hurts.sskirillss.yagm.client.EmissiveModelRegistry;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
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
        renderEmissiveOverlay(poseStack, buffer, state);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderEmissiveOverlay(PoseStack poseStack, MultiBufferSource buffer, BlockState state) {
        BakedModel emissiveModel = EmissiveModelRegistry.getBakedModel(state.getBlock());
        if (emissiveModel == null || !hasAnyQuads(emissiveModel, state)) return;

        poseStack.pushPose();
        applyEmissiveDepthOffset(poseStack);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS)),
                state,
                emissiveModel,
                1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    private static boolean hasAnyQuads(BakedModel model, BlockState state) {
        RandomSource random = RandomSource.create(42L);

        if (!model.getQuads(state, null, random).isEmpty()) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            if (!model.getQuads(state, direction, random).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static void applyEmissiveDepthOffset(PoseStack poseStack) {
        // Inflate emissive pass slightly to prevent z-fighting against base geometry.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(1.001F, 1.001F, 1.001F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(FallingGraveEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
