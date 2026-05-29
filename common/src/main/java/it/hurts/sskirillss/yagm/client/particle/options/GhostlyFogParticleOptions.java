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
public class GhostlyFogParticleOptions implements ParticleOptions {
    private final Vector3f color;
    private final int lifetimeLimit;

    public GhostlyFogParticleOptions(float r, float g, float b, int lifetimeLimit) {
        this(new Vector3f(r, g, b), lifetimeLimit);
    }

    public static final StreamCodec<? super ByteBuf, GhostlyFogParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeFloat(option.color.x());
                buf.writeFloat(option.color.y());
                buf.writeFloat(option.color.z());
                buf.writeInt(option.lifetimeLimit);
            },
            (buf) -> new GhostlyFogParticleOptions(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt())
    );

    public static final MapCodec<GhostlyFogParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(object ->
            object.group(
                    Codec.FLOAT.optionalFieldOf("r", 0.82f).forGetter(p -> p.color.x()),
                    Codec.FLOAT.optionalFieldOf("g", 0.82f).forGetter(p -> p.color.y()),
                    Codec.FLOAT.optionalFieldOf("b", 0.82f).forGetter(p -> p.color.z()),
                    Codec.INT.optionalFieldOf("lifetime_limit", 64).forGetter(GhostlyFogParticleOptions::getLifetimeLimit)
            ).apply(object, GhostlyFogParticleOptions::new)
    );

    @Override
    public @NotNull ParticleType<GhostlyFogParticleOptions> getType() {
        return ParticleRegistry.GHOSTLY_FOG.get();
    }
}
