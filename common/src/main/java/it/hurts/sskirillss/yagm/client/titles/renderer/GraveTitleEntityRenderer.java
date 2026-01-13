package it.hurts.sskirillss.yagm.client.titles.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.client.titles.entity.GraveTitleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GraveTitleEntityRenderer extends EntityRenderer<GraveTitleEntity> {

    public GraveTitleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }


    @Override
    public void render(GraveTitleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Component text = entity.getText();
        if (text == null || text.getString().isEmpty()) {
            return;
        }

        poseStack.pushPose();

        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.ZP.rotationDegrees(180));

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        Font font = getFont();
        String textStr = text.getString();
        float x = -font.width(textStr) / 2f;

        font.drawInBatch(textStr, x, 0, entity.getColor(), false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0x40000000, packedLight);
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(GraveTitleEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}