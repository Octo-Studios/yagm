package it.hurts.sskirillss.yagm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import lombok.Getter;
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
    private final double startY;

    @Getter
    private final int trailColorIn;

    @Getter
    private final int trailColorOut;

    public GraveTrailParticle(ClientLevel level, double x, double y, double z, double xdIn, double ydIn, double zdIn, GraveTrailParticleOptions options, SpriteSet spriteSet) {
        super(level, x, y, z, xdIn, ydIn, zdIn);
        this.spriteSet = spriteSet;
        this.startY = y;

        this.xd = 0.0;
        this.yd = ydIn + random.nextFloat() * 0.002f;
        this.zd = 0.0;
        this.quadSize = 0.001f;
        this.lifetime = 28 + random.nextInt(53);
        this.gravity = 0f;
        this.friction = 0.98f;

        float tint = 0.9f + random.nextFloat() * 0.2f;
        this.rCol = Mth.clamp(options.getColor().x() * tint, 0f, 1f);
        this.gCol = Mth.clamp(options.getColor().y() * tint, 0f, 1f);
        this.bCol = Mth.clamp(options.getColor().z() * tint, 0f, 1f);
        this.alpha = 0.0f;

        int r = Mth.floor(this.rCol * 255f);
        int g = Mth.floor(this.gCol * 255f);
        int b = Mth.floor(this.bCol * 255f);
        this.trailColorIn = 0xFF000000 | (r << 16) | (g << 8) | b;

        int rOut = Mth.floor(this.rCol * 0.7f * 255f);
        int gOut = Mth.floor(this.gCol * 0.7f * 255f);
        int bOut = Mth.floor(this.bCol * 0.7f * 255f);
        this.trailColorOut = 0x99000000 | (rOut << 16) | (gOut << 8) | bOut;

        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public float getQuadSize(float partialTick) {
        return 0.001f;
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

        this.alpha = 0.0f;
        this.setSpriteFromAge(this.spriteSet);

        this.move(this.xd, this.yd, this.zd);
        this.yd *= 0.986f;
        this.xd *= 0.986f;
        this.zd *= 0.986f;

        if (this.y - this.startY >= 3.0) {
            this.remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {}

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
