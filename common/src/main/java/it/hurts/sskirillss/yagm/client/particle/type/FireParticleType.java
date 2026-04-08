package it.hurts.sskirillss.yagm.client.particle.type;

import com.mojang.serialization.MapCodec;
import it.hurts.sskirillss.yagm.client.particle.options.FireParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class FireParticleType extends ParticleType<FireParticleOptions> {
    public FireParticleType() {
        super(false);
    }

    @Override
    public MapCodec<FireParticleOptions> codec() {
        return FireParticleOptions.CODEC;
    }

    @Override
    public StreamCodec<? super RegistryFriendlyByteBuf, FireParticleOptions> streamCodec() {
        return FireParticleOptions.STREAM_CODEC;
    }
}
