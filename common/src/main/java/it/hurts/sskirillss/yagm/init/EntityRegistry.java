package it.hurts.sskirillss.yagm.init;

import it.hurts.sskirillss.yagm.YAGMCommon;

import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.entity.GhostEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(YAGMCommon.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<FallingGraveEntity>> FALLING_GRAVE =
            ENTITIES.register("falling_grave", () ->
                    EntityType.Builder.of(FallingGraveEntity::new, MobCategory.MISC)
                            .sized(0.98f, 0.98f)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("falling_grave"));

    public static final RegistrySupplier<EntityType<GhostEntity>> GHOST =
            ENTITIES.register("ghost", () ->
                    EntityType.Builder.of(GhostEntity::new, MobCategory.MONSTER)
                            .sized(0.45f, 0.60f)
                            .clientTrackingRange(64)
                            .updateInterval(3)
                            .build("ghost"));


    public static void init() {
        ENTITIES.register();
        registerAttributes();
    }

    private static void registerAttributes() {
        EntityAttributeRegistry.register(GHOST, GhostEntity::setCustomAttributes);
    }
}
