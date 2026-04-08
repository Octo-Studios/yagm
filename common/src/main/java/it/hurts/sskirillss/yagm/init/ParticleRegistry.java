package it.hurts.sskirillss.yagm.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.client.particle.options.FireParticleOptions;
import it.hurts.sskirillss.yagm.client.particle.options.GraveTrailParticleOptions;
import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import it.hurts.sskirillss.yagm.client.particle.type.FireParticleType;
import it.hurts.sskirillss.yagm.client.particle.type.GraveTrailParticleType;
import it.hurts.sskirillss.yagm.client.particle.type.GroundDustParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(YAGMCommon.MODID, Registries.PARTICLE_TYPE);
    public static final RegistrySupplier<ParticleType<GroundDustParticleOptions>> GRAVE_DUST_FLAT = PARTICLE_TYPES.register("grave_dust_flat", GroundDustParticleType::new);
    public static final RegistrySupplier<ParticleType<GraveTrailParticleOptions>> GRAVE_TRAIL = PARTICLE_TYPES.register("grave_trail", GraveTrailParticleType::new);
    public static final RegistrySupplier<ParticleType<FireParticleOptions>> CANDLE_FLAME = PARTICLE_TYPES.register("candle_flame", FireParticleType::new);

    public static void init() {
        PARTICLE_TYPES.register();
    }
}
