package it.hurts.sskirillss.yagm.init;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.variant.builder.GraveVariantBuilder;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.component.type.GraveVariantTypes;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;

public class DefaultVariantsRegistry {
    public static void registerAll() {
        // Default grave (fallback for all biomes)
        GraveVariantRegistry.registerDefault(
                GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.DEFAULT.getPath())
                        .displayName("Default")
                        .priority(0)
                        .build()
        );

        // Cold biomes (taiga, snowy)
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.COLD.getPath())
                .displayName("Cold")
                .priority(55)
                .inOverworldLevel()
                .matchBiomes(Biomes.SNOWY_BEACH, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS, Biomes.SNOWY_SLOPES)
                .buildAndRegister();

        // Hot biomes (desert, savanna, badlands)
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.HOT.getPath())
                .displayName("Hot")
                .priority(55)
                .inOverworldLevel()
                .matchBiomes(Biomes.DESERT, Biomes.SAVANNA, Biomes.BADLANDS)
                .candlePositions(new double[]{-0.34375, -0.1875, 0.59375}, new double[]{-0.375, -0.390625, 0.46875})
                .buildAndRegister();

        // Nether biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.NETHER.getPath())
                .displayName("Nether")
                .priority(60)
                .inNetherLevel()
                .buildAndRegister();

        // End biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.END.getPath())
                .displayName("End")
                .priority(60)
                .inEndLevel()
                .candlePositions(new double[]{0.375, -0.34375, 0.65625}, new double[]{-0.375, -0.34375, 0.46875})
                .buildAndRegister();

        // Tropics biomes (jungle)
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.TROPICS.getPath())
                .displayName("Tropics")
                .priority(55)
                .inOverworldLevel()
                .matchBiomeTags(BiomeTags.IS_JUNGLE)
                .candlePositions(new double[]{0.3125, -0.1875, 0.53125}, new double[]{0.28125, -0.390625, 0.40625})
                .buildAndRegister();

        // Ocean biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, GraveVariantTypes.OCEAN.getPath())
                .displayName("Ocean")
                .priority(55)
                .inOverworldLevel()
                .matchBiomeTags(BiomeTags.IS_OCEAN, BiomeTags.IS_DEEP_OCEAN, BiomeTags.IS_BEACH)
                .buildAndRegister();
    }
}
