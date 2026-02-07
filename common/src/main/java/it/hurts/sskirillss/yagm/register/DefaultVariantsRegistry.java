package it.hurts.sskirillss.yagm.register;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.variant.builder.GraveVariantBuilder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;

public class DefaultVariantsRegistry {
    public static void registerAll() {
        // Default grave (fallback for all biomes)
        GraveVariantBuilder.create(YAGMCommon.MODID, "default")
                .displayName("Default")
                .priority(0)
                .buildAndRegister();

        // Cold biomes (taiga, snowy)
        GraveVariantBuilder.create(YAGMCommon.MODID, "cold")
                .displayName("Cold")
                .priority(55)
                .inOverworldLevel()
                .matchBiomes(Biomes.SNOWY_BEACH, Biomes.SNOWY_TAIGA, Biomes.SNOWY_PLAINS, Biomes.SNOWY_SLOPES)
                .buildAndRegister();

        // Hot biomes (desert, savanna, badlands)
        GraveVariantBuilder.create(YAGMCommon.MODID, "hot")
                .displayName("Hot")
                .priority(55)
                .inOverworldLevel()
                .matchBiomes(Biomes.DESERT, Biomes.SAVANNA, Biomes.BADLANDS)
                .buildAndRegister();

        // Nether biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, "nether")
                .displayName("Nether")
                .priority(60)
                .inNetherLevel()
                .buildAndRegister();

        // End biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, "end")
                .displayName("End")
                .priority(60)
                .inEndLevel()
                .buildAndRegister();

        // Tropics biomes (jungle)
        GraveVariantBuilder.create(YAGMCommon.MODID, "tropics")
                .displayName("Tropics")
                .priority(55)
                .inOverworldLevel()
                .matchBiomeTags(BiomeTags.IS_JUNGLE)
                .buildAndRegister();

        // Ocean biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, "ocean")
                .displayName("ocean")
                .priority(55)
                .inOverworldLevel()
                .matchBiomeTags(BiomeTags.IS_OCEAN, BiomeTags.IS_DEEP_OCEAN, BiomeTags.IS_BEACH)
                .buildAndRegister();
    }
}
