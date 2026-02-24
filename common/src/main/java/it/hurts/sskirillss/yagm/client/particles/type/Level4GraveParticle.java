package it.hurts.sskirillss.yagm.client.particles.type;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;


public class Level4GraveParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final double startY;

    protected Level4GraveParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = spriteSet;
        this.startY = y;

        this.friction = 0.985F;
        this.gravity = -0.0025F;
        this.speedUpWhenYMotionIsBlocked = true;
        this.lifetime = 140 + this.random.nextInt(25);
        this.quadSize *= 0.22F + this.random.nextFloat() * 0.06F;
        this.alpha = 0.92F;
        this.xd *= 0.25D;
        this.yd = Math.max(this.yd, 0.022D);
        this.zd *= 0.25D;
        this.pickSprite(spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.removed) {
            return;
        }

        float lifeProgress = (float) this.age / (float) this.lifetime;
        if (lifeProgress < 0.2F) {
            this.alpha = lifeProgress / 0.2F;
        } else {
            this.alpha = 1.0F - ((lifeProgress - 0.2F) / 0.8F) * 0.35F;
        }

        this.yd = Math.min(this.yd + 0.0003D, 0.055D);

        if (this.y - this.startY >= 8.0D) {
            this.remove();
        }
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new Level4GraveParticle(level, x, y, z, xd, yd, zd, spriteSet);
        }
    }
}