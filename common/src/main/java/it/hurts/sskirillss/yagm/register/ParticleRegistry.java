package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(YAGMCommon.MODID, Registries.PARTICLE_TYPE);

    public static final RegistrySupplier<ParticleType<SimpleParticleType>> LANTERN_GLOW = PARTICLE_TYPES.register("lantern_glow", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<ParticleType<SimpleParticleType>> CANDLE_GLOW = PARTICLE_TYPES.register("candle_glow", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<SimpleParticleType> LEVEL4_GRAVE = PARTICLE_TYPES.register("level4_grave", () -> new SimpleParticleType(false));

    public static final RegistrySupplier<SimpleParticleType> CANDLE_FLAME = PARTICLE_TYPES.register("candle_flame", () -> new SimpleParticleType(false));
    public static final RegistrySupplier<SimpleParticleType> SOUL_CANDLE_FLAME = PARTICLE_TYPES.register("soul_candle_flame", () -> new SimpleParticleType(false));


    public static void init() {
        PARTICLE_TYPES.register();
    }
}
