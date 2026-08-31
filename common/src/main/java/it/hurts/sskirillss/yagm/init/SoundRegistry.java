package it.hurts.sskirillss.yagm.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

public class SoundRegistry {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(YAGMCommon.MODID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> GHOST_DEATH = SOUNDS.register("ghost_death", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_death")));
    public static final RegistrySupplier<SoundEvent> GHOST_HURT_1 = SOUNDS.register("ghost_hurt_1", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_hurt_1")));
    public static final RegistrySupplier<SoundEvent> GHOST_HURT_2 = SOUNDS.register("ghost_hurt_2", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_hurt_2")));
    public static final RegistrySupplier<SoundEvent> GHOST_IDLE_1 = SOUNDS.register("ghost_idle_1", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_idle_1")));
    public static final RegistrySupplier<SoundEvent> GHOST_IDLE_2 = SOUNDS.register("ghost_idle_2", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_idle_2")));
    public static final RegistrySupplier<SoundEvent> GHOST_IDLE_3 = SOUNDS.register("ghost_idle_3", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_idle_3")));
    public static final RegistrySupplier<SoundEvent> GHOST_PET_1 = SOUNDS.register("ghost_pet_1", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_pet_1")));
    public static final RegistrySupplier<SoundEvent> GHOST_PET_2 = SOUNDS.register("ghost_pet_2", () -> SoundEvent.createVariableRangeEvent(YAGMCommon.id("ghost_pet_2")));

    public static void init() {
        SOUNDS.register();
    }
}
