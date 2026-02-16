package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.client.particles.spawner.Level4GraveParticleSpawner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
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
        if (!blockEntity.hasLevel()) {
            return;
        }

        Level level = blockEntity.getLevel();
        BlockState state = level.getBlockState(blockEntity.getBlockPos());

        if (state.getBlock() != blockEntity.getBlockState().getBlock()) {
            return;
        }

        renderText(blockEntity, poseStack, buffer, packedLight);
        Level4GraveParticleSpawner.spawn(level, blockEntity, state, level.getRandom());
    }

    private void renderText(GraveStoneBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String ownerName = blockEntity.getOwnerName();
        if (ownerName == null || ownerName.isEmpty() || ownerName.equals("Unknown")) {
            return;
        }

        if (!blockEntity.hasLevel() || blockEntity.isRemoved()) {
            return;
        }

        float textHeight = blockEntity.getTextHeight();
        int textColor = blockEntity.getTextColor();

        if (blockEntity.getVariant() != null) {
            textHeight += blockEntity.getVariant().getTextHeightOffset();
            textColor = blockEntity.getVariant().getTextColor();
        }

        poseStack.pushPose();

        poseStack.translate(0.5D, textHeight, 0.5D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        int textWidth = font.width(ownerName);
        float x = -textWidth / 2.0F;

        int opaqueColor = textColor | 0xFF000000;
        font.drawInBatch(ownerName, x, 0F, opaqueColor, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(GraveStoneBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 100;
    }
}
