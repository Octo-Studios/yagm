package it.hurts.sskirillss.yagm.client.renderer;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.client.model.GhostEntityModel;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;


public class GhostEntityRenderer extends MobRenderer<GhostEntity, GhostEntityModel> {

    public GhostEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new GhostEntityModel(context.bakeLayer(GhostEntityModel.LAYER_LOCATION)), 0.0f);
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
