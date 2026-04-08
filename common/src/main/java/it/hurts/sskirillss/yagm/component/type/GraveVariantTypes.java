package it.hurts.sskirillss.yagm.component.type;

import it.hurts.sskirillss.yagm.YAGMCommon;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;


@SuppressWarnings("all")
@AllArgsConstructor
public enum GraveVariantTypes implements StringRepresentable {
    DEFAULT("default"),
    COLD("cold"),
    HOT("hot"),
    NETHER("nether"),
    END("end"),
    TROPICS("tropics"),
    OCEAN("ocean");

    public static final StringRepresentable.EnumCodec<GraveVariantTypes> CODEC = StringRepresentable.fromEnum(GraveVariantTypes::values);

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

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}
