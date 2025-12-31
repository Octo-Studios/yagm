package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlock;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;

import static it.hurts.sskirillss.yagm.register.BlockEntityRegistry.BLOCK_ENTITIES;


public final class BlockRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK);

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_1 = BLOCKS.register("grave_tier_1", () -> new GraveStoneBlock(
         BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_2 = BLOCKS.register("grave_tier_2", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_3 = BLOCKS.register("grave_tier_3", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_4 = BLOCKS.register("grave_tier_4", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

//    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_5 = BLOCKS.register("grave_tier_5", () -> new GraveStoneBlock(
//            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));


    public static final RegistrySupplier<BlockEntityType<GraveStoneBlockEntity>> GRAVE_STONE_BE =
            BLOCK_ENTITIES.register("grave_stone", () ->
                    BlockEntityType.Builder.of(
                            GraveStoneBlockEntity::new,
                            GRAVESTONE_LEVEL_1.get(),
                            GRAVESTONE_LEVEL_2.get(),
                            GRAVESTONE_LEVEL_3.get(),
                            GRAVESTONE_LEVEL_4.get()
                    ).build(null)
            );

    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> LEVEL_TO_BLOCK = new EnumMap<>(GraveStoneLevels.class);


    static {
        LEVEL_TO_BLOCK.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, GRAVESTONE_LEVEL_1);
        LEVEL_TO_BLOCK.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, GRAVESTONE_LEVEL_2);
        LEVEL_TO_BLOCK.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, GRAVESTONE_LEVEL_3);
        LEVEL_TO_BLOCK.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, GRAVESTONE_LEVEL_4);
    }

    public static Block getBlockForLevel(GraveStoneLevels level) {
        Block block = LEVEL_TO_BLOCK.getOrDefault(level, GRAVESTONE_LEVEL_1).get();
        return block;
    }

    public static void init() {
        BLOCKS.register();
    }
}
