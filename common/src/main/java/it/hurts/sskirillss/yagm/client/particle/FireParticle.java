package it.hurts.sskirillss.yagm.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.hurts.sskirillss.yagm.client.particle.options.FireParticleOptions;
import lombok.Builder;
import lombok.Data;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class FireParticle extends TextureSheetParticle {
    private final Constructor constructor;

    private float oldQuadSize;
    private float currentQuadSize;

    public FireParticle(ClientLevel world, double x, double y, double z, double velocityX, double velocityY, double velocityZ, Constructor constructor) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        setColor(constructor.getColor().getRed() / 255F, constructor.getColor().getGreen() / 255F, constructor.getColor().getBlue() / 255F);
        setSize(constructor.getDiameter(), constructor.getDiameter());
        setAlpha(constructor.getColor().getAlpha() / 255F);
        setLifetime(constructor.getLifetime());

        this.constructor = constructor;

        this.quadSize = constructor.getDiameter();
        this.hasPhysics = constructor.isPhysical();

        this.oldQuadSize = quadSize;
        this.currentQuadSize = quadSize;

        this.xd = velocityX;
        this.yd = velocityY;
        this.zd = velocityZ;
    }

    @Override
    public void tick() {
        this.oldQuadSize = quadSize;
        this.currentQuadSize *= constructor.getScaleModifier();

        xo = x;
        yo = y;
        zo = z;

        oRoll = roll;
        roll += constructor.getRoll();

        var color = this.constructor.getColor();

        setColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F);
        setAlpha(color.getAlpha() / 255f);

        move(xd, yd, zd);

        if (this.age++ >= this.lifetime)
            this.remove();
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @NotNull
    @Override
    public ParticleRenderType getRenderType() {
        return RENDERER_TRANSLUCENT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        this.quadSize = Mth.lerp(partialTicks, oldQuadSize, currentQuadSize);

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        super.render(buffer, renderInfo, partialTicks);
    }


    public static final ParticleRenderType RENDERER_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public BufferBuilder begin(Tesselator tesselator, TextureManager manager) {
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };


    @SuppressWarnings("all")
    @Data
    @Builder
    public static class Constructor {
        @Builder.Default
        private Color color;

        @Builder.Default
        private float diameter = 1F;

        @Builder.Default
        private float roll = 0F;

        @Builder.Default
        private boolean physical = true;

        @Builder.Default
        private int lifetime = 20;

        @Builder.Default
        private float scaleModifier = 1F;

        public static class ConstructorBuilder {
            private Color color = new Color(0xFFFFFFFF, true);

            public ConstructorBuilder color(int color) {
                this.color = new Color(color, true);

                return this;
            }

            public ConstructorBuilder color(float r, float g, float b, float a) {
                return this.color(new Color(r, g, b, a).getRGB());
            }

            public ConstructorBuilder color(float r, float g, float b) {
                return this.color(r, g, b, 1F);
            }

            public ConstructorBuilder color(int r, int g, int b, int a) {
                return this.color(r / 255F, g / 255F, b / 255F, a / 255F);
            }

            public ConstructorBuilder color(int r, int g, int b) {
                return this.color(r, g, b, 0xFF);
            }
        }
    }

    public static class Provider implements ParticleProvider<FireParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(FireParticleOptions options, ClientLevel world, double xPos, double yPos, double zPos, double xVelocity, double yVelocity, double zVelocity) {
            FireParticle particle = new FireParticle(world, xPos, yPos, zPos, xVelocity, yVelocity, zVelocity, options.getData());

            particle.pickSprite(sprites);

            return particle;
        }
    }
}