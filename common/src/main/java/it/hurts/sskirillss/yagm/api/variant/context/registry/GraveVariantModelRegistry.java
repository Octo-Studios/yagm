package it.hurts.sskirillss.yagm.api.variant.context.registry;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;


public class GraveVariantModelRegistry {

    public static List<ResourceLocation> getVariantModels() {
        List<ResourceLocation> models = new ArrayList<>();

        for (IGraveVariant variant : GraveVariantRegistry.getAll()) {
            if (variant == GraveVariantRegistry.getDefaultVariant()) {
                continue;
            }

            for (int level = 1; level <= 4; level++) {
                ResourceLocation modelLocation = variant.getModel(level);
                models.add(modelLocation);
                YAGMCommon.LOGGER.debug("Registering variant model: {}", modelLocation);
            }
        }

        YAGMCommon.LOGGER.info("Registered {} variant models", models.size());
        return models;
    }


    public static List<String> getVariantIds() {
        List<String> ids = new ArrayList<>();

        GraveVariantRegistry.getAll().forEach(variant -> ids.add(variant.getId().toString()));
        return ids;
    }
}