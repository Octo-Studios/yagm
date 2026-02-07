package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Builder
public class GraveStoneBlockRegistry {

    private final Map<GraveStoneLevels, RegistrySupplier<Block>> defaultBlocks;
    private final Map<String, Map<GraveStoneLevels, RegistrySupplier<Block>>> variantToBlocks;

    public Block getBlockForLevel(GraveStoneLevels level) {
        RegistrySupplier<Block> supplier = defaultBlocks.getOrDefault(level, defaultBlocks.get(GraveStoneLevels.GRAVESTONE_LEVEL_1));
        return supplier != null ? supplier.get() : null;
    }

    public Block getBlockForVariant(String variantId, GraveStoneLevels level) {
        if (variantId == null) {
            return getBlockForLevel(level);
        }

        Map<GraveStoneLevels, RegistrySupplier<Block>> variantBlocks = variantToBlocks.get(variantId);
        if (variantBlocks == null) {
            return getBlockForLevel(level);
        }

        RegistrySupplier<Block> blockSupplier = variantBlocks.get(level);
        if (blockSupplier == null) {
            return getBlockForLevel(level);
        }

        return blockSupplier.get();
    }

    public static Map<GraveStoneLevels, RegistrySupplier<Block>> createBlockMap(RegistrySupplier<Block> level1, RegistrySupplier<Block> level2, RegistrySupplier<Block> level3, RegistrySupplier<Block> level4) {
        Map<GraveStoneLevels, RegistrySupplier<Block>> blocks = new EnumMap<>(GraveStoneLevels.class);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_1, level1);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_2, level2);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_3, level3);
        blocks.put(GraveStoneLevels.GRAVESTONE_LEVEL_4, level4);
        return blocks;
    }
}