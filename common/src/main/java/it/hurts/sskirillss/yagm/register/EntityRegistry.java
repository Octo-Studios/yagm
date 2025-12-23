package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class EntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<GraveStoneBlockEntity>> GRAVE_STONE = BLOCK_ENTITIES.register("grave_stone", () -> BlockEntityType.Builder.of(GraveStoneBlockEntity::new, BlockRegistry.GRAVE_STONE.get()).build(null));


    public static void init() {
        BLOCK_ENTITIES.register();
    }
}