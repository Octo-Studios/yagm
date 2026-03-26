package it.hurts.sskirillss.yagm.client.model;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;


public class GhostEntityModel extends HierarchicalModel<GhostEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(YAGMCommon.id("ghost"), "main");

    private final ModelPart root;
    private final ModelPart bb_main;

    public GhostEntityModel(ModelPart root) {
        super(RenderType::entityTranslucentCull);
        this.root = root;
        this.bb_main = root.getChild("bb_main");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bb_main = partdefinition.addOrReplaceChild("bb_main",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r1",
                CubeListBuilder.create()
                        .texOffs(0, 27).addBox(-3.0F, -4.375F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.25F)),
                PartPose.offsetAndRotation(0.0F, -8.625F, 0.75F, 1.5708F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r2",
                CubeListBuilder.create()
                        .texOffs(24, 12).addBox(1.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.25F))
                        .texOffs(24, 0).addBox(1.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, 0.6981F, 0.0F));

        bb_main.addOrReplaceChild("cube_r3",
                CubeListBuilder.create()
                        .texOffs(24, 8).addBox(-4.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.25F))
                        .texOffs(24, 4).addBox(-4.2615F, 1.154F, -1.0F, 3.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, -0.6981F, 0.0F));

        bb_main.addOrReplaceChild("cube_r4",
                CubeListBuilder.create()
                        .texOffs(0, 13).addBox(-3.0F, -3.375F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.5F)),
                PartPose.offsetAndRotation(0.0F, -8.625F, -0.25F, 1.5708F, 0.0F, 0.0F));

        bb_main.addOrReplaceChild("cube_r5",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.375F, -3.0F, 6.0F, 7.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -8.625F, 0.0F, 1.5708F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(GhostEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        bb_main.y = 24.0F + (float) Math.sin(ageInTicks * 0.1) * 0.5F;
        bb_main.zRot = (float) Math.sin(ageInTicks * 0.05) * 0.03F;
    }
}
