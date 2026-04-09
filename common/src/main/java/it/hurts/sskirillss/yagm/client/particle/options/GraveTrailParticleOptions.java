package it.hurts.sskirillss.yagm.client.particle.options;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@Getter
@AllArgsConstructor
public class GraveTrailParticleOptions implements ParticleOptions {
    private final Vector3f color;
    private final float scale;
    private final Vector3f center;

    public GraveTrailParticleOptions(float r, float g, float b, float scale) {
        this(new Vector3f(r, g, b), scale, new Vector3f(0f, 0f, 0f));
    }

    public GraveTrailParticleOptions(float r, float g, float b, float scale, float centerX, float centerY, float centerZ) {
        this(new Vector3f(r, g, b), scale, new Vector3f(centerX, centerY, centerZ));
    }

    public static final StreamCodec<? super ByteBuf, GraveTrailParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeFloat(option.color.x());
                buf.writeFloat(option.color.y());
                buf.writeFloat(option.color.z());
                buf.writeFloat(option.scale);
                buf.writeFloat(option.center.x());
                buf.writeFloat(option.center.y());
                buf.writeFloat(option.center.z());
            },
            (buf) -> new GraveTrailParticleOptions(
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat(),
                    buf.readFloat()
            )
    );

    public static final MapCodec<GraveTrailParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(object ->
            object.group(
                    Codec.FLOAT.optionalFieldOf("r", 0.82f).forGetter(p -> p.color.x()),
                    Codec.FLOAT.optionalFieldOf("g", 0.82f).forGetter(p -> p.color.y()),
                    Codec.FLOAT.optionalFieldOf("b", 0.82f).forGetter(p -> p.color.z()),
                    Codec.FLOAT.optionalFieldOf("scale", 1.0f).forGetter(p -> p.scale),
                    Codec.FLOAT.optionalFieldOf("center_x", 0.0f).forGetter(p -> p.center.x()),
                    Codec.FLOAT.optionalFieldOf("center_y", 0.0f).forGetter(p -> p.center.y()),
                    Codec.FLOAT.optionalFieldOf("center_z", 0.0f).forGetter(p -> p.center.z())
            ).apply(object, GraveTrailParticleOptions::new)
    );

    @Override
    public @NotNull ParticleType<GraveTrailParticleOptions> getType() {
        return ParticleRegistry.GRAVE_TRAIL.get();
    }
}
