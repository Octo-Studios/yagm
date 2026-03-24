package it.hurts.sskirillss.yagm.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GroundDustParticle extends TextureSheetParticle {
    private static final float HALF_PI = Mth.PI / 2f;
    private final SpriteSet spriteSet;
    private final float sourceScale;

    public GroundDustParticle(ClientLevel level, double x, double y, double z, double xdIn, double ydIn, double zdIn, GroundDustParticleOptions options, SpriteSet spriteSet) {
        super(level, x, y, z, xdIn, ydIn, zdIn);
        this.spriteSet = spriteSet;
        this.sourceScale = options.getScale();

        this.xd = xdIn + (random.nextFloat() - 0.5f) * 0.006f;
        this.yd = ydIn + random.nextFloat() * 0.004f;
        this.zd = zdIn + (random.nextFloat() - 0.5f) * 0.006f;
        this.quadSize = (0.9f + random.nextFloat() * 0.45f) * options.getScale();
        this.lifetime = this.sourceScale <= 0.8f ? 16 + random.nextInt(11) : 34 + random.nextInt(20);
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
        this.yd *= 0.5f;
        this.xd *= 0.985f;
        this.zd *= 0.985f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        renderHorizontal(buffer, camera, partialTick, false);
        renderHorizontal(buffer, camera, partialTick, true);
    }

    private void renderHorizontal(VertexConsumer consumer, Camera camera, float partialTick, boolean backFace) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cam.z());

        Quaternionf q = new Quaternionf();
        if (backFace) {
            q.mul(Axis.YP.rotation(-(float) Math.PI));
            q.mul(Axis.XP.rotation(HALF_PI));
        } else {
            q.mul(Axis.XP.rotation(-HALF_PI));
        }

        Vector3f[] corners = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F),
                new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, -1.0F, 0.0F)
        };

        float size = this.getQuadSize(partialTick);
        for (Vector3f corner : corners) {
            corner.rotate(q);
            corner.mul(size);
            corner.add(x, y, z);
        }

        int light = this.getLightColor(partialTick);
        makeCornerVertex(consumer, corners[0], this.getU1(), this.getV1(), light);
        makeCornerVertex(consumer, corners[1], this.getU1(), this.getV0(), light);
        makeCornerVertex(consumer, corners[2], this.getU0(), this.getV0(), light);
        makeCornerVertex(consumer, corners[3], this.getU0(), this.getV1(), light);
    }

    private void makeCornerVertex(VertexConsumer consumer, Vector3f vec, float u, float v, int light) {
        consumer.addVertex(vec.x(), vec.y(), vec.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
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
            return new GroundDustParticle(level, x, y, z, xd, yd, zd, options, this.spriteSet);
        }
    }
}
