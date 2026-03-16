package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EmissiveModelRegistry {

    private static final Map<Block, ResourceLocation> BLOCK_TO_EMISSIVE = new HashMap<>();
    private static final Map<ResourceLocation, BakedModel> BAKED_MODELS = new HashMap<>();

    public static void init() {
        registerMapping(BlockRegistry.GRAVESTONE_LEVEL_4,   "block/grave_tier_4_emissive");
        registerMapping(BlockRegistry.COLD_GRAVESTONE_4,    "block/cold_grave_tier_4_emissive");
        registerMapping(BlockRegistry.HOT_GRAVESTONE_4,     "block/hot_grave_tier_4_emissive");
        registerMapping(BlockRegistry.NETHER_GRAVESTONE_4,  "block/nether_grave_tier_4_emissive");
        registerMapping(BlockRegistry.TROPICS_GRAVESTONE_4, "block/tropics_grave_tier_4_emissive");
        registerMapping(BlockRegistry.END_GRAVESTONE_4,     "block/end_grave_tier_4_emissive");
        registerMapping(BlockRegistry.OCEAN_GRAVESTONE_4,   "block/ocean_grave_tier_4_emissive");
    }

    private static void registerMapping(RegistrySupplier<Block> supplier, String path) {
        BLOCK_TO_EMISSIVE.put(supplier.get(), ResourceLocation.fromNamespaceAndPath("yagm", path));
    }

    public static void onResourcesReloaded() {
        BAKED_MODELS.clear();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        for (Map.Entry<Block, ResourceLocation> entry : BLOCK_TO_EMISSIVE.entrySet()) {
            ResourceLocation id = entry.getValue();
            for (String variant : new String[]{"", "inventory", "emissive", "standalone"}) {
                ModelResourceLocation mrl = new ModelResourceLocation(id, variant);
                BakedModel model = modelManager.getModel(mrl);
                boolean isMissing = (model == modelManager.getMissingModel());
                System.out.println("Trying: " + mrl + " -> missing: " + isMissing);
                if (!isMissing) {
                    BAKED_MODELS.put(id, model);
                    System.out.println("SUCCESS: " + mrl);
                    break;
                }
            }
        }
        System.out.println("Total baked emissive models: " + BAKED_MODELS.size());
    }

    @Nullable
    public static BakedModel getBakedModel(Block block) {
        ResourceLocation id = BLOCK_TO_EMISSIVE.get(block);
        if (id == null) return null;
        return BAKED_MODELS.get(id);
    }

    public static List<ResourceLocation> getModelIds() {
        return new ArrayList<>(BLOCK_TO_EMISSIVE.values());
    }
}