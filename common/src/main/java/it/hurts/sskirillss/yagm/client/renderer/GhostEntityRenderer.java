package it.hurts.sskirillss.yagm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.client.model.GhostEntityModel;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;


public class GhostEntityRenderer extends MobRenderer<GhostEntity, EntityModel<GhostEntity>> {

    public GhostEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostEntityModel<>(context.bakeLayer(GhostEntityModel.LAYER_LOCATION)), 0.0f);
    }

    @Override
    public void render(GhostEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();

        var time = entity.tickCount + partialTicks;

        var hoverOffset = Mth.sin(time * 0.12F) * 0.12F;
        var driftX = Mth.sin(time * 0.04F) * 0.03F;

        poseStack.translate(driftX, hoverOffset, 0.0D);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    protected void setupRotations(GhostEntity entity, PoseStack poseStack, float ageInTicks, float rotY, float partialTick, float scale) {
        super.setupRotations(entity, poseStack, ageInTicks, rotY, partialTick, scale);
        float pitch = Mth.lerp(partialTick, entity.xBodyRotO, entity.xBodyRot);
        poseStack.mulPose(Axis.XN.rotationDegrees(pitch));
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
