package it.hurts.sskirillss.yagm.client.particle;

import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class GroundDustParticle extends TextureSheetParticle {
    private final SpriteSet spriteSet;
    private final boolean fixedSize;
    private final float startSize;
    private final float endSize;

    public GroundDustParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, GroundDustParticleOptions options, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.spriteSet = spriteSet;
        float requestedScale = options.getScale();
        float scale = Math.abs(requestedScale);
        this.fixedSize = requestedScale < 0.0f;

        if (fixedSize) {
            float base = 1.0f * scale;
            this.startSize = base;
            this.endSize = base;
        } else {
            float base = (0.38f + random.nextFloat() * 0.24f) * scale;
            boolean shrink = random.nextFloat() < 0.45f;
            this.startSize = shrink ? base * (1.25f + random.nextFloat() * 0.30f) : base * (0.70f + random.nextFloat() * 0.20f);
            this.endSize = shrink ? base * (0.62f + random.nextFloat() * 0.18f) : base * (1.35f + random.nextFloat() * 0.45f);
        }

        this.xd = xd + (random.nextFloat() - 0.5f) * 0.006f;
        this.yd = yd + 0.002f + random.nextFloat() * 0.0035f;
        this.zd = zd + (random.nextFloat() - 0.5f) * 0.006f;

        this.quadSize = this.startSize;
        this.lifetime = (int) (40 + random.nextInt(26) + scale * 14);
        this.gravity = 0f;
        this.friction = 0.965f;
        this.hasPhysics = false;

        float tint = 0.92f + random.nextFloat() * 0.16f;
        float whiten = random.nextFloat() * 0.22f;
        float baseR = Mth.clamp(options.getColor().x() * tint, 0f, 1f);
        float baseG = Mth.clamp(options.getColor().y() * tint, 0f, 1f);
        float baseB = Mth.clamp(options.getColor().z() * tint, 0f, 1f);
        this.rCol = Mth.clamp(baseR + whiten * (1f - baseR), 0f, 1f);
        this.gCol = Mth.clamp(baseG + whiten * (1f - baseG), 0f, 1f);
        this.bCol = Mth.clamp(baseB + whiten * (1f - baseB), 0f, 1f);
        this.alpha = 0f;

        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public float getQuadSize(float partialTick) {
        return quadSize;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;

        if (age++ >= lifetime) {
            remove();
            return;
        }

        float t = age / (float) lifetime;
        float fadeIn = Mth.clamp(t / 0.20f, 0f, 1f);
        float fadeOut = 1f - Mth.clamp((t - 0.58f) / 0.42f, 0f, 1f);
        alpha = 0.34f * fadeIn * fadeOut;
        quadSize = fixedSize ? startSize : Mth.lerp(Mth.clamp(t, 0f, 1f), startSize, endSize);

        setSpriteFromAge(spriteSet);
        move(xd, yd, zd);

        yd *= 0.92f;
        xd *= 0.985f;
        zd *= 0.985f;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<GroundDustParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(@NotNull GroundDustParticleOptions options, @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GroundDustParticle(level, x, y, z, xd, yd, zd, options, spriteSet);
        }
    }
}
