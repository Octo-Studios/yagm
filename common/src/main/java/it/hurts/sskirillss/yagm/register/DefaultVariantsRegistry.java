package it.hurts.sskirillss.yagm.register;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.variant.builder.GraveVariantBuilder;
import net.minecraft.world.level.biome.Biomes;

public class DefaultVariantsRegistry {
    public static void registerAll() {
        GraveVariantBuilder.create(YAGMCommon.MODID, "cold")
                .displayName("Cold")
                .priority(55)
                .inOverworldLevel()
                .getIsMatch(ctx ->
                        ctx.isBiome(Biomes.SNOWY_PLAINS) ||
                        ctx.isBiome(Biomes.SNOWY_TAIGA) ||
                        ctx.isBiome(Biomes.SNOWY_SLOPES) ||
                        ctx.isBiome(Biomes.FROZEN_PEAKS) ||
                        ctx.isBiome(Biomes.ICE_SPIKES) ||
                        ctx.isBiome(Biomes.GROVE))
                .modelPattern("cold_grave_tier_{level}")
                .buildAndRegister();
    }
}
