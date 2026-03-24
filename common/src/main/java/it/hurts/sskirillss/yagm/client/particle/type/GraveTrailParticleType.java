package it.hurts.sskirillss.yagm.client.particle.type;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class GraveTrailParticleType extends ParticleType<GraveTrailParticleOptions> {
    public GraveTrailParticleType() {
        super(false);
    }

    @Override
    public @NotNull MapCodec<GraveTrailParticleOptions> codec() {
        return GraveTrailParticleOptions.MAP_CODEC;
    }

    @Override
    public @NotNull StreamCodec<? super ByteBuf, GraveTrailParticleOptions> streamCodec() {
        return GraveTrailParticleOptions.STREAM_CODEC;
    }
}
