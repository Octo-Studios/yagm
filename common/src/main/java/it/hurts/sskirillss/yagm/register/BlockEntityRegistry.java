package it.hurts.sskirillss.yagm.register;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.fallinggrave.GraveStoneBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class BlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<GraveStoneBlockEntity>> GRAVE_STONE =
            BLOCK_ENTITIES.register("grave_stone", () ->
                    BlockEntityType.Builder.of(
                            GraveStoneBlockEntity::new,
                            BlockRegistry.GRAVESTONE_LEVEL_1.get(),
                            BlockRegistry.GRAVESTONE_LEVEL_2.get(),
                            BlockRegistry.GRAVESTONE_LEVEL_3.get(),
                            BlockRegistry.GRAVESTONE_LEVEL_4.get(),
                            BlockRegistry.HOT_GRAVESTONE_1.get(),
                            BlockRegistry.HOT_GRAVESTONE_2.get(),
                            BlockRegistry.HOT_GRAVESTONE_3.get(),
                            BlockRegistry.HOT_GRAVESTONE_4.get(),
                            BlockRegistry.COLD_GRAVESTONE_1.get(),
                            BlockRegistry.COLD_GRAVESTONE_2.get(),
                            BlockRegistry.COLD_GRAVESTONE_3.get(),
                            BlockRegistry.COLD_GRAVESTONE_4.get(),
                            BlockRegistry.NETHER_GRAVESTONE_1.get(),
                            BlockRegistry.NETHER_GRAVESTONE_2.get(),
                            BlockRegistry.NETHER_GRAVESTONE_3.get(),
                            BlockRegistry.NETHER_GRAVESTONE_4.get(),
                            BlockRegistry.TROPICS_GRAVESTONE_1.get(),
                            BlockRegistry.TROPICS_GRAVESTONE_2.get(),
                            BlockRegistry.TROPICS_GRAVESTONE_3.get(),
                            BlockRegistry.TROPICS_GRAVESTONE_4.get(),
                            BlockRegistry.END_GRAVESTONE_1.get(),
                            BlockRegistry.END_GRAVESTONE_2.get(),
                            BlockRegistry.END_GRAVESTONE_3.get(),
                            BlockRegistry.END_GRAVESTONE_4.get(),
                            BlockRegistry.OCEAN_GRAVESTONE_1.get(),
                            BlockRegistry.OCEAN_GRAVESTONE_2.get(),
                            BlockRegistry.OCEAN_GRAVESTONE_3.get(),
                            BlockRegistry.OCEAN_GRAVESTONE_4.get()
                    ).build(null)
            );

    public static void init() {
        BLOCK_ENTITIES.register();
    }
}