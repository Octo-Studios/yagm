package it.hurts.sskirillss.yagm.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.client.particle.options.GhostlyFogParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class GhostlyFogParticle extends TextureSheetParticle {
    public static final ParticleRenderType RENDERER = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager manager) {
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public String toString() {
            return YAGMCommon.MODID + ":ghostly_fog";
        }
    };

    private final float initialSize;
    private final float targetSize;
    private final float rotation;
    private final float spin;

    protected GhostlyFogParticle(ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed, GhostlyFogParticleOptions options) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);

        int lifetimeLimit = Math.max(1, options.getLifetimeLimit());
        this.lifetime = Math.min(lifetimeLimit, 46 + this.random.nextInt(24));

        this.initialSize = 0.55F + this.random.nextFloat() * 0.35F;
        this.targetSize = this.initialSize * (1.8F + this.random.nextFloat() * 0.8F);
        this.quadSize = this.initialSize;

        this.rotation = this.random.nextFloat() * Mth.TWO_PI;
        this.spin = (this.random.nextFloat() - 0.5F) * 0.012F;

        this.gravity = -0.002F;
        this.friction = 0.93F;
        this.hasPhysics = false;
        this.alpha = 0F;

        this.rCol = options.getColor().x();
        this.gCol = options.getColor().y();
        this.bCol = options.getColor().z();

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
    }

    @Override
    public void tick() {
        super.tick();

        float progress = this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(progress / 0.18F, 0F, 1F);
        float fadeOut = Mth.clamp((1F - progress) / 0.18F, 0F, 1F);

        this.alpha = 0.15F * fadeIn * fadeOut;
        this.quadSize = Mth.lerp(Mth.clamp(progress, 0F, 1F), this.initialSize, this.targetSize);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - camera.getPosition().x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - camera.getPosition().y()) + 0.015F;
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - camera.getPosition().z());
        float angle = this.rotation + (this.age + partialTicks) * this.spin;
        int light = this.getLightColor(partialTicks);
        float size = this.getQuadSize(partialTicks);
        Quaternionf rotation = new Quaternionf(camera.rotation());

        rotation.rotateZ(angle);

        Vector3f[] vertices = new Vector3f[] {
                new Vector3f(-1F, -1F, 0F),
                new Vector3f(-1F, 1F, 0F),
                new Vector3f(1F, 1F, 0F),
                new Vector3f(1F, -1F, 0F)
        };

        for (Vector3f vertex : vertices) {
            vertex.rotate(rotation).mul(size).add(x, y, z);
        }

        this.addVertex(buffer, vertices[0].x(), vertices[0].y(), vertices[0].z(), this.getU1(), this.getV1(), light);
        this.addVertex(buffer, vertices[3].x(), vertices[3].y(), vertices[3].z(), this.getU0(), this.getV1(), light);
        this.addVertex(buffer, vertices[2].x(), vertices[2].y(), vertices[2].z(), this.getU0(), this.getV0(), light);
        this.addVertex(buffer, vertices[1].x(), vertices[1].y(), vertices[1].z(), this.getU1(), this.getV0(), light);
    }

    private void addVertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return RENDERER;
    }

    public static class Provider implements ParticleProvider<GhostlyFogParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(GhostlyFogParticleOptions options, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            GhostlyFogParticle particle = new GhostlyFogParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, options);

            particle.pickSprite(this.sprites);

            return particle;
        }
    }
}
