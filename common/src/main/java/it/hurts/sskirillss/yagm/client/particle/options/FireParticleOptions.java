package it.hurts.sskirillss.yagm.client.particle.options;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.hurts.sskirillss.yagm.client.particle.FireParticle;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public class FireParticleOptions implements ParticleOptions {

    @Getter
    private final FireParticle.Constructor data;

    public FireParticleOptions(int color, float diameter, int lifetime, float roll, float scaleModifier) {
        this.data = FireParticle.Constructor.builder()
                .color(color)
                .diameter(diameter)
                .lifetime(lifetime)
                .roll(roll)
                .scaleModifier(scaleModifier)
                .build();
    }

    public FireParticleOptions(FireParticle.Constructor data) {
        this.data = data;
    }

    @NotNull
    @Override
    public ParticleType<FireParticleOptions> getType() {
        return ParticleRegistry.CANDLE_FLAME.get();
    }

    public static final MapCodec<FireParticleOptions> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.INT.fieldOf("color").forGetter(options -> options.getData().getColor().getRGB()),
                    Codec.FLOAT.fieldOf("diameter").forGetter(options -> options.getData().getDiameter()),
                    Codec.INT.fieldOf("lifetime").forGetter(options -> options.getData().getLifetime()),
                    Codec.FLOAT.fieldOf("roll").forGetter(options -> options.getData().getRoll()),
                    Codec.FLOAT.fieldOf("scaleModifier").forGetter(options -> options.getData().getScaleModifier())
            ).apply(instance, FireParticleOptions::new));

    public static final StreamCodec<ByteBuf, FireParticleOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, options -> options.getData().getColor().getRGB(),
            ByteBufCodecs.FLOAT, options -> options.getData().getDiameter(),
            ByteBufCodecs.INT, options -> options.getData().getLifetime(),
            ByteBufCodecs.FLOAT, options -> options.getData().getRoll(),
            ByteBufCodecs.FLOAT, options -> options.getData().getScaleModifier(),
            FireParticleOptions::new
    );
}
