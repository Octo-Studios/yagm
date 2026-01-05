package it.hurts.sskirillss.yagm.register;

import it.hurts.sskirillss.yagm.YAGMCommon;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.blocks.gravestones.FallingGraveEntity;
import it.hurts.sskirillss.yagm.client.titles.entity.GraveTitleEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(YAGMCommon.MODID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<GraveTitleEntity>> GRAVE_TITLE =
            ENTITIES.register("grave_title", () ->
                    EntityType.Builder.<GraveTitleEntity>of(GraveTitleEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(64)
                            .updateInterval(20)
                            .build("grave_title"));

    public static final RegistrySupplier<EntityType<FallingGraveEntity>> FALLING_GRAVE =
            ENTITIES.register("falling_grave", () ->
                    EntityType.Builder.<FallingGraveEntity>of(FallingGraveEntity::new, MobCategory.MISC)
                            .sized(0.98f, 0.98f)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("falling_grave"));

    public static void init() {
        ENTITIES.register();
    }
}