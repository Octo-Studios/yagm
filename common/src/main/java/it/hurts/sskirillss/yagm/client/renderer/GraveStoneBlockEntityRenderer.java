package it.hurts.sskirillss.yagm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.block.entity.GraveStoneBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class GraveStoneBlockEntityRenderer implements BlockEntityRenderer<GraveStoneBlockEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public GraveStoneBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(GraveStoneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        BlockState worldState = level.getBlockState(pos);

        if (worldState.hasProperty(GraveStoneBlock.HALF) && worldState.getValue(GraveStoneBlock.HALF) == DoubleBlockHalf.UPPER)
            return;

        if (worldState.isAir()) return;

        BlockState modelState = worldState;
        if (modelState.hasProperty(GraveStoneBlock.HALF)) {
            modelState = modelState.setValue(GraveStoneBlock.HALF, DoubleBlockHalf.LOWER);
        }

        if (modelState.hasProperty(GraveStoneBlock.WATERLOGGED)) {
            modelState = modelState.setValue(GraveStoneBlock.WATERLOGGED, false);
        }

        BakedModel model = blockRenderer.getBlockModel(modelState);
        int light = LevelRenderer.getLightColor(level, pos);

        blockRenderer.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)), modelState, model, 1.0F, 1.0F, 1.0F, light, OverlayTexture.NO_OVERLAY);
    }

    @Override
    public boolean shouldRenderOffScreen(GraveStoneBlockEntity blockEntity) {
        return true;
    }
}
