package it.hurts.sskirillss.yagm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class GraveTrailParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;

    public GraveTrailParticle(ClientLevel level, double x, double y, double z, double xdIn, double ydIn, double zdIn, GraveTrailParticleOptions options, SpriteSet spriteSet) {
        super(level, x, y, z, xdIn, ydIn, zdIn);
        this.spriteSet = spriteSet;

        this.xd = xdIn + (random.nextFloat() - 0.5f) * 0.01f;
        this.yd = ydIn + 0.03f + random.nextFloat() * 0.05f;
        this.zd = zdIn + (random.nextFloat() - 0.5f) * 0.01f;
        this.quadSize = (0.8f + random.nextFloat() * 0.35f) * options.getScale();
        this.lifetime = 26 + random.nextInt(16);
        this.gravity = 0f;
        this.friction = 0.98f;

        float tint = 0.9f + random.nextFloat() * 0.2f;
        this.rCol = Mth.clamp(options.getColor().x() * tint, 0f, 1f);
        this.gCol = Mth.clamp(options.getColor().y() * tint, 0f, 1f);
        this.bCol = Mth.clamp(options.getColor().z() * tint, 0f, 1f);
        this.alpha = 0.0f;

        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public float getQuadSize(float partialTick) {
        float age01 = (this.age + partialTick) / (float) this.lifetime;
        float fadeIn = Mth.clamp(age01 * 3.2f, 0f, 1f);
        float fadeOut = 1f - Mth.clamp((age01 - 0.55f) / 0.45f, 0f, 1f);
        return this.quadSize * (0.9f + fadeIn * 0.2f) * fadeOut;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float age01 = this.age / (float) this.lifetime;
        float fadeIn = Mth.clamp(age01 * 4f, 0f, 1f);
        float fadeOut = 1f - Mth.clamp((age01 - 0.55f) / 0.45f, 0f, 1f);
        this.alpha = 0.9f * fadeIn * fadeOut;
        this.setSpriteFromAge(this.spriteSet);

        this.move(this.xd, this.yd, this.zd);
        this.yd *= 0.988f;
        this.xd *= 0.985f;
        this.zd *= 0.985f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        super.render(buffer, camera, partialTick);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<GraveTrailParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull GraveTrailParticleOptions options, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GraveTrailParticle(level, x, y, z, xd, yd, zd, options, this.spriteSet);
        }
    }
}
