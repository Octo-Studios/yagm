//package it.hurts.sskirillss.yagm.client.particles.type;
//
//import net.minecraft.client.multiplayer.ClientLevel;
//import net.minecraft.client.particle.*;
//import net.minecraft.core.particles.SimpleParticleType;
//
//public class LanternGlowParticle extends TextureSheetParticle {
//
//    private final SpriteSet sprites;
//    private final float baseAlpha;
//    private final float flickerSpeed;
//
//    protected LanternGlowParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
//        super(level, x, y, z, 0, 0, 0);
//        this.sprites = sprites;
//
//        this.xd = 0;
//        this.yd = 0;
//        this.zd = 0;
//
//        this.quadSize = 0.15f + random.nextFloat() * 0.05f;
//        this.lifetime = 20;
//        this.rCol = 1.0f;
//        this.gCol = 0.8f + random.nextFloat() * 0.15f;
//        this.bCol = 0.4f + random.nextFloat() * 0.2f;
//        this.baseAlpha = 0.6f + random.nextFloat() * 0.3f;
//        this.alpha = baseAlpha;
//        this.flickerSpeed = 0.1f + random.nextFloat() * 0.15f;
//        this.gravity = 0;
//        this.setSpriteFromAge(sprites);
//    }
//
//    @Override
//    public void tick() {
//        this.xo = this.x;
//        this.yo = this.y;
//        this.zo = this.z;
//
//        if (this.age++ >= this.lifetime) {
//            this.remove();
//            return;
//        }
//
//        float flicker = (float) Math.sin(this.age * flickerSpeed + random.nextFloat() * 0.5f);
//        this.alpha = baseAlpha + flicker * 0.2f;
//
//        this.quadSize = 0.15f + (float) Math.sin(this.age * flickerSpeed * 0.5f) * 0.02f;
//    }
//
//    @Override
//    public ParticleRenderType getRenderType() {
//        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
//    }
//
//    @Override
//    public int getLightColor(float partialTick) {
//        return 0xF000F0;
//    }
//
//    public static class Provider implements ParticleProvider<SimpleParticleType> {
//        private final SpriteSet sprites;
//
//        public Provider(SpriteSet sprites) {
//            this.sprites = sprites;
//        }
//
//        @Override
//        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
//            return new LanternGlowParticle(level, x, y, z, sprites);
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
