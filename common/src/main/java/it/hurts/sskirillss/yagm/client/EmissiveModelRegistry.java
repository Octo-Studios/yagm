package it.hurts.sskirillss.yagm.client;

import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.init.BlockRegistry;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@Slf4j
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
        onResourcesReloaded(Minecraft.getInstance().getModelManager());
    }

    public static void onResourcesReloaded(ModelManager modelManager) {
        BAKED_MODELS.clear();
        for (Map.Entry<Block, ResourceLocation> entry : BLOCK_TO_EMISSIVE.entrySet()) {
            ResourceLocation id = entry.getValue();
            ModelResourceLocation[] candidates = {
                    new ModelResourceLocation(id, "standalone"),
                    new ModelResourceLocation(id, ""),
                    ModelResourceLocation.inventory(id),
                    new ModelResourceLocation(id, "emissive")
            };

            for (ModelResourceLocation mrl : candidates) {
                BakedModel model = modelManager.getModel(mrl);
                if (model != modelManager.getMissingModel()) {
                    BAKED_MODELS.put(id, model);
                    log.debug("[YAGM] Loaded emissive model: {}", mrl);
                    break;
                }
            }
        }
        log.debug("[YAGM] Total baked emissive models: {}", BAKED_MODELS.size());
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
