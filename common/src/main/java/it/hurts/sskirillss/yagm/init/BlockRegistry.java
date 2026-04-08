package it.hurts.sskirillss.yagm.init;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.block.GraveStoneShape;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import it.hurts.sskirillss.yagm.util.VariantUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public final class BlockRegistry {
    private static VariantUtils registry;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK);

    private static RegistrySupplier<Block> registerGrave(String id, GraveStoneShape shape) {
        return BLOCKS.register(id, () -> new GraveStoneBlock(BlockBehaviour.Properties.of().strength(0.4F, 6.0F).requiresCorrectToolForDrops().noOcclusion(), shape));
    }

    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_1 = registerGrave("grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_2 = registerGrave("grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_3 = registerGrave("grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> GRAVESTONE_LEVEL_4 = registerGrave("grave_tier_4", GraveStoneShape.TIER_4);

    public static final RegistrySupplier<Block> COLD_GRAVESTONE_1 = registerGrave("cold_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> COLD_GRAVESTONE_2 = registerGrave("cold_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> COLD_GRAVESTONE_3 = registerGrave("cold_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> COLD_GRAVESTONE_4 = registerGrave("cold_grave_tier_4", GraveStoneShape.TIER_4);

    public static final RegistrySupplier<Block> HOT_GRAVESTONE_1 = registerGrave("hot_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> HOT_GRAVESTONE_2 = registerGrave("hot_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> HOT_GRAVESTONE_3 = registerGrave("hot_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> HOT_GRAVESTONE_4 = registerGrave("hot_grave_tier_4", GraveStoneShape.TIER_4);

    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_1 = registerGrave("nether_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_2 = registerGrave("nether_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_3 = registerGrave("nether_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> NETHER_GRAVESTONE_4 = registerGrave("nether_grave_tier_4", GraveStoneShape.TIER_4);

    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_1 = registerGrave("tropics_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_2 = registerGrave("tropics_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_3 = registerGrave("tropics_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> TROPICS_GRAVESTONE_4 = registerGrave("tropics_grave_tier_4", GraveStoneShape.TIER_4);

    public static final RegistrySupplier<Block> END_GRAVESTONE_1 = registerGrave("end_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> END_GRAVESTONE_2 = registerGrave("end_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> END_GRAVESTONE_3 = registerGrave("end_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> END_GRAVESTONE_4 = registerGrave("end_grave_tier_4", GraveStoneShape.TIER_4);
    
    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_1 = registerGrave("ocean_grave_tier_1", GraveStoneShape.TIER_1);
    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_2 = registerGrave("ocean_grave_tier_2", GraveStoneShape.TIER_2);
    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_3 = registerGrave("ocean_grave_tier_3", GraveStoneShape.TIER_3);
    public static final RegistrySupplier<Block> OCEAN_GRAVESTONE_4 = registerGrave("ocean_grave_tier_4", GraveStoneShape.TIER_4);


    private static String createVariantId(GraveVariantTypes variant) {
        return variant.getId();
    }

    public static VariantUtils getRegistry() {
        if (registry == null) {
            Map<String, Map<GraveStoneLevels, RegistrySupplier<Block>>> variants = new HashMap<>();

            variants.put(createVariantId(GraveVariantTypes.COLD), VariantUtils.createBlockMap(COLD_GRAVESTONE_1, COLD_GRAVESTONE_2, COLD_GRAVESTONE_3, COLD_GRAVESTONE_4));
            variants.put(createVariantId(GraveVariantTypes.HOT), VariantUtils.createBlockMap(HOT_GRAVESTONE_1, HOT_GRAVESTONE_2, HOT_GRAVESTONE_3, HOT_GRAVESTONE_4));
            variants.put(createVariantId(GraveVariantTypes.NETHER), VariantUtils.createBlockMap(NETHER_GRAVESTONE_1, NETHER_GRAVESTONE_2, NETHER_GRAVESTONE_3, NETHER_GRAVESTONE_4));
            variants.put(createVariantId(GraveVariantTypes.TROPICS), VariantUtils.createBlockMap(TROPICS_GRAVESTONE_1, TROPICS_GRAVESTONE_2, TROPICS_GRAVESTONE_3, TROPICS_GRAVESTONE_4));
            variants.put(createVariantId(GraveVariantTypes.END), VariantUtils.createBlockMap(END_GRAVESTONE_1, END_GRAVESTONE_2, END_GRAVESTONE_3, END_GRAVESTONE_4));
            variants.put(createVariantId(GraveVariantTypes.OCEAN), VariantUtils.createBlockMap(OCEAN_GRAVESTONE_1, OCEAN_GRAVESTONE_2, OCEAN_GRAVESTONE_3, OCEAN_GRAVESTONE_4));

            registry = VariantUtils.builder().defaultBlocks(VariantUtils.createBlockMap(GRAVESTONE_LEVEL_1, GRAVESTONE_LEVEL_2, GRAVESTONE_LEVEL_3, GRAVESTONE_LEVEL_4)).variantToBlocks(variants).build();
        }

        return registry;
    }

    public static Block getVariant(String variantId, GraveStoneLevels level) {
        return getRegistry().getVariant(variantId, level);
    }

    public static Block[] getGraves() {
        return Arrays.asList(
                GRAVESTONE_LEVEL_1.get(), GRAVESTONE_LEVEL_2.get(), GRAVESTONE_LEVEL_3.get(), GRAVESTONE_LEVEL_4.get(),
                COLD_GRAVESTONE_1.get(), COLD_GRAVESTONE_2.get(), COLD_GRAVESTONE_3.get(), COLD_GRAVESTONE_4.get(),
                HOT_GRAVESTONE_1.get(), HOT_GRAVESTONE_2.get(), HOT_GRAVESTONE_3.get(), HOT_GRAVESTONE_4.get(),
                NETHER_GRAVESTONE_1.get(), NETHER_GRAVESTONE_2.get(), NETHER_GRAVESTONE_3.get(), NETHER_GRAVESTONE_4.get(),
                TROPICS_GRAVESTONE_1.get(), TROPICS_GRAVESTONE_2.get(), TROPICS_GRAVESTONE_3.get(), TROPICS_GRAVESTONE_4.get(),
                END_GRAVESTONE_1.get(), END_GRAVESTONE_2.get(), END_GRAVESTONE_3.get(), END_GRAVESTONE_4.get(),
                OCEAN_GRAVESTONE_1.get(), OCEAN_GRAVESTONE_2.get(), OCEAN_GRAVESTONE_3.get(), OCEAN_GRAVESTONE_4.get()
        ).toArray(new Block[0]);
    }

    public static void init() {
        BLOCKS.register();
    }
}