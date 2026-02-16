package it.hurts.sskirillss.yagm.data_components.gravestones_types;

import it.hurts.sskirillss.yagm.YAGMCommon;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;


@SuppressWarnings("all")
@AllArgsConstructor
public enum GraveVariantTypes {
    DEFAULT("default"),
    COLD("cold"),
    HOT("hot"),
    NETHER("nether"),
    END("end"),
    TROPICS("tropics"),
    OCEAN("ocean");

    private final String path;

    public String getPath() {
        return path;
    }

    public String getId() {
        return YAGMCommon.MODID + ":" + path;
    }

    public ResourceLocation getResourceLocation() {
        return ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, path);
    }
}
