package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlock;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;


public class GraveStoneBlockEntityRenderer implements BlockEntityRenderer<GraveStoneBlockEntity> {

    private final Font font;
    private final BlockRenderDispatcher blockRenderer;

    public GraveStoneBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(GraveStoneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        renderVariantModel(blockEntity, poseStack, buffer, packedLight, packedOverlay);
        renderText(blockEntity, poseStack, buffer, packedLight);
    }


    private void renderVariantModel(GraveStoneBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        IGraveVariant variant = blockEntity.getVariant();
        IGraveVariant defaultVariant = GraveVariantRegistry.getDefaultVariant();

        if (variant == null || variant == defaultVariant) {
            return;
        }

        GraveStoneLevels level = blockEntity.getGraveLevel();
        if (level == null) {
            level = GraveStoneLevels.GRAVESTONE_LEVEL_1;
        }


        ResourceLocation modelLocation = variant.getModel(level.getLevel());

        ModelResourceLocation modelResLoc = new ModelResourceLocation(modelLocation, "standalone");

        BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelResLoc);
        BakedModel missingModel = Minecraft.getInstance().getModelManager().getMissingModel();


        if (model == missingModel) {
            return;
        }

        poseStack.pushPose();


        BlockState state = blockEntity.getBlockState();
        if (state.hasProperty(GraveStoneBlock.FACING)) {
            Direction facing = state.getValue(GraveStoneBlock.FACING);
            float rotation = getRotationFromDirection(facing);

            poseStack.translate(0.5, 0, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
            poseStack.translate(-0.5, 0, -0.5);
        }


        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.cutout());
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), vertexConsumer, state, model, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);

        poseStack.popPose();
    }

    private void renderText(GraveStoneBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String ownerName = blockEntity.getOwnerName();
        if (ownerName == null || ownerName.isEmpty() || ownerName.equals("Unknown")) {
            return;
        }

        IGraveVariant variant = blockEntity.getVariant();

        float textHeight = blockEntity.getTextHeight();
        int textColor = blockEntity.getTextColor();


        if (variant != null) {
            textHeight += variant.getTextHeightOffset();
            textColor = variant.getTextColor();
        }

        poseStack.pushPose();

        poseStack.translate(0.5D, textHeight, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        int textWidth = font.width(ownerName);
        float x = -textWidth / 2.0F;

        font.drawInBatch(ownerName, x, 0F, textColor, false, poseStack.last().pose(), buffer, Font.DisplayMode.SEE_THROUGH, 0x20000000, packedLight);

        poseStack.popPose();
    }

    private float getRotationFromDirection(Direction direction) {
        return switch (direction) {
            case SOUTH -> 180f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };
    }

    @Override
    public boolean shouldRenderOffScreen(GraveStoneBlockEntity blockEntity) {
        return true;
    }
}