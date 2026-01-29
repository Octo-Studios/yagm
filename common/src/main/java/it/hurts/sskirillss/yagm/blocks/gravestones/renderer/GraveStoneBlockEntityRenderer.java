package it.hurts.sskirillss.yagm.blocks.gravestones.renderer;


import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.GraveStoneBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


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
        spawnLanternParticles(level, blockEntity, state, level.getRandom());
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

    private static final double[][] LANTERN_OFFSETS = {
            {0.21875, 0.3125,  0.25  },
            {0.71875, 0.25,    0.28125},
            {0.15625, 0.34375, 0.3125},
            {0.21875, 0.375,   0.3125}
    };

    private static final double TOP_OFFSET = 0.12D;

    private static final int SPAWN_CHANCE = 200;
    private static final int PARTICLES_PER_SPAWN = 1;

    public static final Map<String, ParticleOptions> CUSTOM_VARIANT_PARTICLES = new HashMap<>();
    static {
        // CUSTOM_VARIANT_PARTICLES.put("yagm:hot", ParticleTypes.FLAME);
    }


    private void spawnLanternParticles(Level level, GraveStoneBlockEntity blockEntity, BlockState state, RandomSource rand) {
        if (level == null || !level.isClientSide) return;
        if (rand.nextInt(SPAWN_CHANCE) != 0) return;

        Direction facing = null;
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        Vec3 cam = player.getEyePosition(1.0F);

        BlockPos pos = blockEntity.getBlockPos();

        double bestDistSq = Double.POSITIVE_INFINITY;
        int bestIndex = -1;
        double bestPx = 0, bestPy = 0, bestPz = 0;

        for (int i = 0; i < LANTERN_OFFSETS.length; i++) {
            double[] off = LANTERN_OFFSETS[i];
            double[] rot = rotateXZByFacing(off[0], off[2], facing);
            double localX = rot[0];
            double localZ = rot[1];
            double worldX = pos.getX() + localX;
            double worldY = pos.getY() + off[1] + TOP_OFFSET;
            double worldZ = pos.getZ() + localZ;

            if (localX > 0.5) {
                double dx = cam.x - worldX;
                double dy = cam.y - worldY;
                double dz = cam.z - worldZ;
                double distSq = dx*dx + dy*dy + dz*dz;
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    bestIndex = i;
                    bestPx = worldX;
                    bestPy = worldY;
                    bestPz = worldZ;
                }
            }
        }


        if (bestIndex < 0) {
            double bestLocalX = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < LANTERN_OFFSETS.length; i++) {
                double[] off = LANTERN_OFFSETS[i];
                double[] rot = rotateXZByFacing(off[0], off[2], facing);
                double localX = rot[0];
                double localZ = rot[1];
                if (localX > bestLocalX) {
                    bestLocalX = localX;
                    bestIndex = i;
                    bestPx = pos.getX() + localX;
                    bestPy = pos.getY() + off[1] + TOP_OFFSET;
                    bestPz = pos.getZ() + localZ;
                }
            }
        }

        if (bestIndex < 0) return;

        ParticleOptions particle = ParticleTypes.FLAME;
        if (blockEntity != null && blockEntity.getVariant() != null && blockEntity.getVariant().getId() != null) {
            String id = blockEntity.getVariant().getId().toString().toLowerCase(Locale.ROOT);
            ParticleOptions custom = CUSTOM_VARIANT_PARTICLES.get(id);
            if (custom != null) particle = custom;
        }

        for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
            double jitterX = (rand.nextDouble() - 0.5) * 0.02;
            double jitterY = rand.nextDouble() * 0.02;
            double jitterZ = (rand.nextDouble() - 0.5) * 0.02;

            double vx = (rand.nextDouble() - 0.5) * 0.005;
            double vy = 0.01 + rand.nextDouble() * 0.015;
            double vz = (rand.nextDouble() - 0.5) * 0.005;

            level.addParticle(particle, bestPx + jitterX, bestPy + jitterY, bestPz + jitterZ, vx, vy, vz);
        }

    }

    private double[] rotateXZByFacing(double x, double z, Direction facing) {
        int degrees = 0;
        if (facing != null) {
            degrees = switch (facing) {
                case NORTH -> 0;
                case EAST -> 90;
                case SOUTH -> 180;
                case WEST -> 270;
                default -> 0;
            };
        }

        double cx = 0.5, cz = 0.5;
        double dx = x - cx;
        double dz = z - cz;
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;
        return new double[]{cx + rx, cz + rz};
    }
}
