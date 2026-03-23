package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.entity.GraveStoneEntity;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.client.EmissiveFilteredModel;
import it.hurts.sskirillss.yagm.client.EmissiveModelRegistry;
import it.hurts.sskirillss.yagm.client.particles.spawner.CandleFlameSpawner;
import it.hurts.sskirillss.yagm.client.particles.spawner.Level4GraveParticleSpawner;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

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
        if (level == null) return;

        BlockState state = resolveRenderState(entity, level);
        if (state == null || state.isAir()) return;

        BakedModel model = blockRenderer.getBlockModel(state);
        renderBaseModel(poseStack, buffer, state, model, packedLight);
        renderEmissiveOverlay(poseStack, buffer, state, model);

        renderText(entity, poseStack, buffer, packedLight);
        Level4GraveParticleSpawner.spawn(level, entity, state, level.getRandom());
        CandleFlameSpawner.spawn(level, entity.getBoundPos(), state);

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
        if (block == null) {
            block = BlockRegistry.getBlockForLevel(entity.getGraveLevel());
        }

        return block != null ? block.defaultBlockState() : worldState;
    }

    private void renderBaseModel(PoseStack poseStack, MultiBufferSource buffer, BlockState state, BakedModel model, int packedLight) {
        BakedModel baseOnlyModel = new EmissiveFilteredModel(model, false);

        poseStack.pushPose();
        poseStack.translate(-0.5, 0, -0.5);
        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(ItemBlockRenderTypes.getRenderType(state, false)),
                state,
                baseOnlyModel,
                1.0F, 1.0F, 1.0F,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
    }

    private void renderEmissiveOverlay(PoseStack poseStack, MultiBufferSource buffer, BlockState state, BakedModel model) {
        BakedModel emissiveModel = EmissiveModelRegistry.getBakedModel(state.getBlock());
        if (emissiveModel == null || !hasAnyQuads(emissiveModel, state)) {
            emissiveModel = new EmissiveFilteredModel(model, true);
        }
        if (!hasAnyQuads(emissiveModel, state)) return;

        poseStack.pushPose();
        poseStack.translate(-0.5, 0, -0.5);
        applyEmissiveDepthOffset(poseStack);

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                buffer.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS)),
                state,
                emissiveModel,
                1.0F, 1.0F, 1.0F,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }

    private static boolean hasAnyQuads(BakedModel model, BlockState state) {
        RandomSource random = RandomSource.create(42L);

        if (!model.getQuads(state, null, random).isEmpty()) {
            return true;
        }

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            if (!model.getQuads(state, direction, random).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static void applyEmissiveDepthOffset(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(1.001F, 1.001F, 1.001F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }

    private void renderText(GraveStoneEntity entity, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        String ownerName = entity.getOwnerName();
        if (ownerName == null || ownerName.isEmpty() || ownerName.equals("Unknown")) return;
        if (entity.isRemoved()) return;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getTextHeight(), 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025F, -0.025F, 0.025F);

        int textWidth = font.width(ownerName);
        font.drawInBatch(ownerName, -textWidth / 2.0F, 0F, entity.getTextColor() | 0xFF000000, false, poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);

        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(GraveStoneEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
    }

    @Override
    public boolean shouldRender(GraveStoneEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }
}
