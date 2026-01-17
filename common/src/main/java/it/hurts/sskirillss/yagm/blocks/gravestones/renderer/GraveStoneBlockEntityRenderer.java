package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;


public class GraveStoneBlockEntityRenderer implements BlockEntityRenderer<GraveStoneBlockEntity> {

    private final Font font;
    private final BlockRenderDispatcher blockRenderer;

    public GraveStoneBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(GraveStoneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!blockEntity.hasLevel() || blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() != blockEntity.getBlockState().getBlock()) {
            return;
        }
        renderText(blockEntity, poseStack, buffer, packedLight);
    }


    private void renderText(GraveStoneBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String ownerName = blockEntity.getOwnerName();
        if (ownerName == null || ownerName.isEmpty() || ownerName.equals("Unknown")) {
            return;
        }

        if (!blockEntity.hasLevel() || blockEntity.isRemoved()) {
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

    @Override
    public boolean shouldRenderOffScreen(GraveStoneBlockEntity blockEntity) {
        return true;
    }
}