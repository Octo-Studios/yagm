package it.hurts.sskirillss.yagm.client.particle.type;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class GroundDustParticleType extends ParticleType<GroundDustParticleOptions> {

    public GroundDustParticleType() {
        super(false);
    }

    @Override
    public @NotNull MapCodec<GroundDustParticleOptions> codec() {
        return GroundDustParticleOptions.MAP_CODEC;
    }

    @Override
    public @NotNull StreamCodec<? super ByteBuf, GroundDustParticleOptions> streamCodec() {
        return GroundDustParticleOptions.STREAM_CODEC;
    }
}
