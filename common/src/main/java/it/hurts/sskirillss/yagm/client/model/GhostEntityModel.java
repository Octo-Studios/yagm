package it.hurts.sskirillss.yagm.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;


public class GhostEntityModel <T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(YAGMCommon.id("ghost"), "main");

    private final ModelPart bb_main;

    public GhostEntityModel(ModelPart root) {
        super(RenderType::entityTranslucentCull);
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));

        PartDefinition cube_r1 = bb_main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 27).addBox(-3.0F, -4.375F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.25F)), PartPose.offsetAndRotation(0.0F, -8.625F, 0.75F, 1.5708F, 0.0F, 0.0F));

        PartDefinition cube_r2 = bb_main.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(24, 4).addBox(-4.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, -0.6981F, 0.0F));

        PartDefinition cube_r3 = bb_main.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(24, 0).addBox(1.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, 0.6981F, 0.0F));

        PartDefinition cube_r4 = bb_main.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.375F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        bb_main.y = 24.0F + (float) Math.sin(ageInTicks * 0.1) * 0.5F;
        bb_main.zRot = (float) Math.sin(ageInTicks * 0.05) * 0.03F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        bb_main.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
