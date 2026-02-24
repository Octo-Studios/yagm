package it.hurts.sskirillss.yagm.client.particles.type;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class CandleFlameParticle extends TextureSheetParticle {

    private final SpriteSet sprites;
    private final float baseAlpha;
    private final float flickerSpeed;
    private final float baseScale;
    private final double baseX;
    private final double baseZ;

    protected CandleFlameParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0, 0, 0);
        this.sprites = sprites;
        this.baseX = x;
        this.baseZ = z;

        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        this.baseScale = 0.12f + random.nextFloat() * 0.03f;
        this.quadSize = baseScale;
        this.lifetime = 40 + this.random.nextInt(20);

        this.baseAlpha = 0.85f + random.nextFloat() * 0.15f;
        this.alpha = baseAlpha;
        this.flickerSpeed = 0.3f + random.nextFloat() * 0.2f;

        this.gravity = 0;
        this.hasPhysics = false;

        this.setSpriteFromAge(sprites);
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

        float animProgress = (float) this.age / 4.0f;
        int spriteIndex = ((int) animProgress) % 4;
        this.setSprite(this.sprites.get(spriteIndex, 3));

        float flicker = (float) Math.sin(this.age * flickerSpeed + random.nextFloat() * 0.3f);
        this.alpha = Math.max(0.4f, baseAlpha + flicker * 0.25f);

        this.quadSize = baseScale + (float) Math.sin(this.age * flickerSpeed * 0.7f) * 0.015f;

        double swayAmount = 0.003;
        double swayX = Math.sin(this.age * 0.15) * swayAmount;
        double swayZ = Math.cos(this.age * 0.12) * swayAmount;
        this.x = baseX + swayX;
        this.z = baseZ + swayZ;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new CandleFlameParticle(level, x, y, z, sprites);
        }
    }
}