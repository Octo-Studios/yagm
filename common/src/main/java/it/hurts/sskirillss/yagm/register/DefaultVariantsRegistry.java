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
                .getIsMatch(ctx -> ctx.isBiome(Biomes.SNOWY_BEACH) || ctx.isBiome(Biomes.SNOWY_TAIGA) || ctx.isBiome(Biomes.SNOWY_PLAINS) || ctx.isBiome(Biomes.SNOWY_SLOPES))
                .buildAndRegister();

        // Hot biomes (desert, savanna, badlands)
        GraveVariantBuilder.create(YAGMCommon.MODID, "hot")
                .displayName("Hot")
                .priority(55)
                .inOverworldLevel()
                .getIsMatch(ctx -> ctx.isBiome(Biomes.DESERT) || ctx.isBiome(Biomes.SAVANNA) || ctx.isBiome(Biomes.BADLANDS))
                .buildAndRegister();

        // Nether biomes
        GraveVariantBuilder.create(YAGMCommon.MODID, "nether")
                .displayName("Nether")
                .priority(60)
                .inNetherLevel()
                .buildAndRegister();

        // Tropics biomes (jungle)
        GraveVariantBuilder.create(YAGMCommon.MODID, "tropics")
                .displayName("Tropics")
                .priority(55)
                .inOverworldLevel()
                .matchBiomeTag(BiomeTags.IS_JUNGLE)
                .buildAndRegister();
    }
}
