package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.block.shape.GraveStoneShape;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

import static it.hurts.sskirillss.yagm.register.GraveStoneBlockRegistry.createBlockMap;


public final class BlockRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK);


    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_1 = BLOCKS.register("grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_2 = BLOCKS.register("grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_3 = BLOCKS.register("grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_4 = BLOCKS.register("grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // Cold variant
    public static final RegistrySupplier<Block> COLD_GRAVESTONE_1 = BLOCKS.register("cold_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_2 = BLOCKS.register("cold_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_3 = BLOCKS.register("cold_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_4 = BLOCKS.register("cold_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // Hot variant
    public static final RegistrySupplier<Block> HOT_GRAVESTONE_1 = BLOCKS.register("hot_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_2 = BLOCKS.register("hot_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_3 = BLOCKS.register("hot_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_4 = BLOCKS.register("hot_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // Nether variant
    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_1 = BLOCKS.register("nether_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_2 = BLOCKS.register("nether_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_3 = BLOCKS.register("nether_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_4 = BLOCKS.register("nether_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // Tropics variant
    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_1 = BLOCKS.register("tropics_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_2 = BLOCKS.register("tropics_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_3 = BLOCKS.register("tropics_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_4 = BLOCKS.register("tropics_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // End variant
    public static final RegistrySupplier<Block> END_GRAVESTONE_1 = BLOCKS.register("end_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> END_GRAVESTONE_2 = BLOCKS.register("end_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> END_GRAVESTONE_3 = BLOCKS.register("end_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> END_GRAVESTONE_4 = BLOCKS.register("end_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));

    // Ocean variant
    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_1 = BLOCKS.register("ocean_grave_tier_1",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_1.getNorthSouth(), GraveStoneShape.TIER_1.getEastWest()));

    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_2 = BLOCKS.register("ocean_grave_tier_2",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(),  GraveStoneShape.TIER_2.getNorthSouth(), GraveStoneShape.TIER_2.getEastWest()));

    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_3 = BLOCKS.register("ocean_grave_tier_3",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_3.getNorthSouth(), GraveStoneShape.TIER_3.getEastWest()));

    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_4 = BLOCKS.register("ocean_grave_tier_4",
            () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops(), GraveStoneShape.TIER_4.getNorthSouth(), GraveStoneShape.TIER_4.getEastWest()));


    private static GraveStoneBlockRegistry registry;

    private static String createVariantId(String variant) {
        return YAGMCommon.MODID + ":" + variant;
    }

    public static GraveStoneBlockRegistry getRegistry() {
        if (registry == null) {
            Map<String, Map<GraveStoneLevels, RegistrySupplier<Block>>> variants = new HashMap<>();
            variants.put(createVariantId("cold"), createBlockMap(COLD_GRAVESTONE_1, COLD_GRAVESTONE_2, COLD_GRAVESTONE_3, COLD_GRAVESTONE_4));
            variants.put(createVariantId("hot"), createBlockMap(HOT_GRAVESTONE_1, HOT_GRAVESTONE_2, HOT_GRAVESTONE_3, HOT_GRAVESTONE_4));
            variants.put(createVariantId("nether"), createBlockMap(NETHER_GRAVESTONE_1, NETHER_GRAVESTONE_2, NETHER_GRAVESTONE_3, NETHER_GRAVESTONE_4));
            variants.put(createVariantId("tropics"), createBlockMap(TROPICS_GRAVESTONE_1, TROPICS_GRAVESTONE_2, TROPICS_GRAVESTONE_3, TROPICS_GRAVESTONE_4));
            variants.put(createVariantId("end"), createBlockMap(END_GRAVESTONE_1, END_GRAVESTONE_2, END_GRAVESTONE_3, END_GRAVESTONE_4));
            variants.put(createVariantId("ocean"), createBlockMap(OCEAN_GRAVESTONE_1, OCEAN_GRAVESTONE_2, OCEAN_GRAVESTONE_3, OCEAN_GRAVESTONE_4));

            registry = GraveStoneBlockRegistry.builder().defaultBlocks(createBlockMap(GRAVESTONE_LEVEL_1, GRAVESTONE_LEVEL_2, GRAVESTONE_LEVEL_3, GRAVESTONE_LEVEL_4)).variantToBlocks(variants).build();
        }
        return registry;
    }

    public static Block getBlockForLevel(GraveStoneLevels level) {
        return getRegistry().getBlockForLevel(level);
    }

    public static Block getBlockForVariant(String variantId, GraveStoneLevels level) {
        return getRegistry().getBlockForVariant(variantId, level);
    }

    public static void init() {
        BLOCKS.register();
    }
}
