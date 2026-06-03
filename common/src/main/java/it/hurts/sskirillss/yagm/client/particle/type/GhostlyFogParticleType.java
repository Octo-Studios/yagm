package it.hurts.sskirillss.yagm.client.particle.type;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.yagm.client.particle.options.GhostlyFogParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class GhostlyFogParticleType extends ParticleType<GhostlyFogParticleOptions> {
    public GhostlyFogParticleType() {
        super(false);
    }

    @Override
    public @NotNull MapCodec<GhostlyFogParticleOptions> codec() {
        return GhostlyFogParticleOptions.MAP_CODEC;
    }

    @Override
    public @NotNull StreamCodec<? super ByteBuf, GhostlyFogParticleOptions> streamCodec() {
        return GhostlyFogParticleOptions.STREAM_CODEC;
    }
}
