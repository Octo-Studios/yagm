package it.hurts.sskirillss.yagm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.entity.GraveStoneEntity;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class GraveStoneEntityRenderer extends EntityRenderer<GraveStoneEntity> {

    private final Font font;
    private final BlockRenderDispatcher blockRenderer;

    public GraveStoneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.font = context.getFont();
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(GraveStoneEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Level level = entity.level();

        BlockState state = resolveRenderState(entity, level);

        if (state.isAir()) {
            return;
        }

        renderBaseModel(poseStack, buffer, state, blockRenderer.getBlockModel(state), packedLight);
        renderOwnerName(entity, poseStack, buffer, packedLight);

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private BlockState resolveRenderState(GraveStoneEntity entity, Level level) {
        BlockState worldState = level.getBlockState(entity.getBoundPos());

        if (!worldState.isAir()) {
            return worldState;
        }

        String variantId = null;
        IGraveVariant variant = entity.getVariant();
        if (variant != null && variant.getId() != null) {
            variantId = variant.getId().toString();
        }

        Block block = BlockRegistry.getBlockForVariant(variantId, entity.getGraveLevel());
        if (block == null) block = BlockRegistry.getBlockForLevel(entity.getGraveLevel());
        return block != null ? block.defaultBlockState() : worldState;
    }

    private void renderBaseModel(PoseStack poseStack, MultiBufferSource buffer, BlockState state, BakedModel model, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5, 0, -0.5);
        blockRenderer.getModelRenderer().renderModel(poseStack.last(), buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)), state, model, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    private void renderOwnerName(GraveStoneEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String ownerName = entity.getOwnerName();
        if (ownerName == null || ownerName.isEmpty() || "Unknown".equals(ownerName)) return;
        if (entity.isRemoved()) return;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getTextHeight(), 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        Matrix4f matrix = poseStack.last().pose();
        float x = -font.width(ownerName) / 2.0F;
        int textColor = entity.getTextColor() | 0xFF000000;
        int bgColor = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;

        font.drawInBatch(ownerName, x, 0F, textColor, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        font.drawInBatch(ownerName, x, 0F, textColor, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(GraveStoneEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GraveStoneEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }
}
