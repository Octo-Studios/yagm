package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlock;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


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


    //SKINS
    public static final RegistrySupplier<Block> COLD_GRAVESTONE_1 = BLOCKS.register("cold_grave_tier_1", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_2 = BLOCKS.register("cold_grave_tier_2", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_3 = BLOCKS.register("cold_grave_tier_3", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_4 = BLOCKS.register("cold_grave_tier_4", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_1 = BLOCKS.register("hot_grave_tier_1", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_2 = BLOCKS.register("hot_grave_tier_2", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_3 = BLOCKS.register("hot_grave_tier_3", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_4 = BLOCKS.register("hot_grave_tier_4", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_1 = BLOCKS.register("nether_grave_tier_1", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_2 = BLOCKS.register("nether_grave_tier_2", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_3 = BLOCKS.register("nether_grave_tier_3", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_4 = BLOCKS.register("nether_grave_tier_4", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_1 = BLOCKS.register("tropics_grave_tier_1", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_2 = BLOCKS.register("tropics_grave_tier_2", () -> new GraveStoneBlock(
            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));


//    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_5 = BLOCKS.register("grave_tier_5", () -> new GraveStoneBlock(
//            BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));

    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> default_blocks = new EnumMap<>(GraveStoneLevels.class);
    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> coldBlocks = new EnumMap<>(GraveStoneLevels.class);
    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> hotBlocks = new EnumMap<>(GraveStoneLevels.class);
    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> netherBlocks = new EnumMap<>(GraveStoneLevels.class);
    private static final Map<GraveStoneLevels, RegistrySupplier<Block>> tropicsBlocks = new EnumMap<>(GraveStoneLevels.class);
    // Biome variant blocks mapping: variant_id -> level -> block
    private static final Map<String, Map<GraveStoneLevels, RegistrySupplier<Block>>> VARIANT_TO_BLOCKS = new HashMap<>();

    static {
        default_blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, GRAVESTONE_LEVEL_1);
        default_blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, GRAVESTONE_LEVEL_2);
        default_blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, GRAVESTONE_LEVEL_3);
        default_blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, GRAVESTONE_LEVEL_4);

        // Cold variant

        coldBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, COLD_GRAVESTONE_1);
        coldBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, COLD_GRAVESTONE_2);
        coldBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, COLD_GRAVESTONE_3);
        coldBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, COLD_GRAVESTONE_4);
        VARIANT_TO_BLOCKS.put("yagm:cold", coldBlocks);

        // Hot variant
        hotBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, HOT_GRAVESTONE_1);
        hotBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, HOT_GRAVESTONE_2);
        hotBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, HOT_GRAVESTONE_3);
        hotBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, HOT_GRAVESTONE_4);
        VARIANT_TO_BLOCKS.put("yagm:hot", hotBlocks);

        // Nether variant
        netherBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, NETHER_GRAVESTONE_1);
        netherBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, NETHER_GRAVESTONE_2);
        netherBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, NETHER_GRAVESTONE_3);
        netherBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, NETHER_GRAVESTONE_4);
        VARIANT_TO_BLOCKS.put("yagm:nether", netherBlocks);

        // Tropics variant
        tropicsBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, TROPICS_GRAVESTONE_1);
        tropicsBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, TROPICS_GRAVESTONE_2);
        tropicsBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, null);
        tropicsBlocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, null);
        VARIANT_TO_BLOCKS.put("yagm:tropics", tropicsBlocks);
    }

    public static Block getBlockForLevel(GraveStoneLevels level) {
        Block block = default_blocks.getOrDefault(level, GRAVESTONE_LEVEL_1).get();
        return block;
    }

    public static Block getBlockForVariant(String variantId, GraveStoneLevels level) {
        if (variantId == null) {
            return getBlockForLevel(level);
        }

        Map<GraveStoneLevels, RegistrySupplier<Block>> variantBlocks = VARIANT_TO_BLOCKS.get(variantId);
        if (variantBlocks == null) {
            return getBlockForLevel(level);
        }

        RegistrySupplier<Block> blockSupplier = variantBlocks.get(level);
        if (blockSupplier == null) {
            return getBlockForLevel(level);
        }

        return blockSupplier.get();
    }

    public static void init() {
        BLOCKS.register();
    }
}
