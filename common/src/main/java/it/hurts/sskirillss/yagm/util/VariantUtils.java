package it.hurts.sskirillss.yagm.util;

import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Getter
@Builder
public class VariantUtils {

    public static final Map<String, ResourceLocation> VARIANT_PREFIXES = Map.of(
            "cold_", GraveVariantTypes.COLD.getResourceLocation(),
            "hot_", GraveVariantTypes.HOT.getResourceLocation(),
            "nether_", GraveVariantTypes.NETHER.getResourceLocation(),
            "end_", GraveVariantTypes.END.getResourceLocation(),
            "tropics_", GraveVariantTypes.TROPICS.getResourceLocation(),
            "ocean_", GraveVariantTypes.OCEAN.getResourceLocation()
    );

    private final Map<GraveStoneLevels, RegistrySupplier<Block>> defaultBlocks;
    private final Map<String, Map<GraveStoneLevels, RegistrySupplier<Block>>> variantToBlocks;

    public static String createVariantId(GraveVariantTypes variant) {
        return variant.getId();
    }

    public static Block getVariantId(String variantId, GraveStoneLevels level) {
        return BlockRegistry.getRegistry().getVariant(variantId, level);
    }

    public Block getLevel(GraveStoneLevels level) {
        RegistrySupplier<Block> supplier = defaultBlocks.getOrDefault(level, defaultBlocks.get(GraveStoneLevels.GRAVESTONE_LEVEL_1));
        return supplier != null ? supplier.get() : null;
    }

    public Block getVariant(String variantId, GraveStoneLevels level) {
        if (variantId == null) {
            return getLevel(level);
        }

        Map<GraveStoneLevels, RegistrySupplier<Block>> variantBlocks = variantToBlocks.get(variantId);
        if (variantBlocks == null) {
            return getLevel(level);
        }

        RegistrySupplier<Block> blockSupplier = variantBlocks.get(level);
        if (blockSupplier == null) {
            return getLevel(level);
        }

        return blockSupplier.get();
    }

    public static Block[] getGraves() {
        return Arrays.asList(
                BlockRegistry.GRAVESTONE_LEVEL_1.get(), BlockRegistry.GRAVESTONE_LEVEL_2.get(), BlockRegistry.GRAVESTONE_LEVEL_3.get(), BlockRegistry.GRAVESTONE_LEVEL_4.get(),
                BlockRegistry.COLD_GRAVESTONE_1.get(), BlockRegistry.COLD_GRAVESTONE_2.get(), BlockRegistry.COLD_GRAVESTONE_3.get(), BlockRegistry.COLD_GRAVESTONE_4.get(),
                BlockRegistry.HOT_GRAVESTONE_1.get(), BlockRegistry.HOT_GRAVESTONE_2.get(), BlockRegistry.HOT_GRAVESTONE_3.get(), BlockRegistry.HOT_GRAVESTONE_4.get(),
                BlockRegistry.NETHER_GRAVESTONE_1.get(), BlockRegistry.NETHER_GRAVESTONE_2.get(), BlockRegistry.NETHER_GRAVESTONE_3.get(), BlockRegistry.NETHER_GRAVESTONE_4.get(),
                BlockRegistry.TROPICS_GRAVESTONE_1.get(), BlockRegistry.TROPICS_GRAVESTONE_2.get(), BlockRegistry.TROPICS_GRAVESTONE_3.get(), BlockRegistry.TROPICS_GRAVESTONE_4.get(),
                BlockRegistry.END_GRAVESTONE_1.get(), BlockRegistry.END_GRAVESTONE_2.get(), BlockRegistry.END_GRAVESTONE_3.get(), BlockRegistry.END_GRAVESTONE_4.get(),
                BlockRegistry.OCEAN_GRAVESTONE_1.get(), BlockRegistry.OCEAN_GRAVESTONE_2.get(), BlockRegistry.OCEAN_GRAVESTONE_3.get(), BlockRegistry.OCEAN_GRAVESTONE_4.get()
        ).toArray(new Block[0]);
    }

    public static Map<GraveStoneLevels, RegistrySupplier<Block>> createBlockMap(RegistrySupplier<Block> level1, RegistrySupplier<Block> level2, RegistrySupplier<Block> level3, RegistrySupplier<Block> level4) {
        Map<GraveStoneLevels, RegistrySupplier<Block>> blocks = new EnumMap<>(GraveStoneLevels.class);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, level1);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, level2);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, level3);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, level4);
        return blocks;
    }

    public static float[] getVariantColor(String variantPath) {
        return switch (variantPath != null ? variantPath : "default") {
            case "cold" -> new float[]{0.72f, 0.82f, 0.92f};
            case "hot" -> new float[]{0.96f, 0.57f, 0.28f};
            case "nether" -> new float[]{0.83f, 0.24f, 0.24f};
            case "end" -> new float[]{0.74f, 0.66f, 0.96f};
            case "ocean" -> new float[]{0.34f, 0.74f, 0.93f};
            case "tropics" -> new float[]{0.43f, 0.88f, 0.58f};
            default -> new float[]{0.82f, 0.82f, 0.82f};
        };
    }
}