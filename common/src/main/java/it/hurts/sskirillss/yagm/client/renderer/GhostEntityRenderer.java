package it.hurts.sskirillss.yagm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.client.model.GhostEntityModel;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;


public class GhostEntityRenderer extends MobRenderer<GhostEntity, GhostEntityModel> {

    public GhostEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostEntityModel(context.bakeLayer(GhostEntityModel.LAYER_LOCATION)), 0.0f);
    }

    @Override
    public void render(GhostEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        var time = entity.tickCount + partialTicks;

        var hoverOffset = Mth.sin(time * 0.12F) * 0.12F;

        var swayZ = Mth.sin(time * 0.08F) * 5.0F;
        var swayY = Mth.cos(time * 0.06F) * 5.0F;

        var driftX = Mth.sin(time * 0.04F) * 0.03F;

        poseStack.translate(driftX, hoverOffset, 0.0D);

        poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
        poseStack.mulPose(Axis.YP.rotationDegrees(swayY));

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    protected int getBlockLightLevel(GhostEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(GhostEntity entity) {
        String moodTexture = entity.getMoodTextureName();
        return YAGMCommon.id("textures/entity/" + moodTexture + ".png");
    }

}
