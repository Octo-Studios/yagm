package it.hurts.sskirillss.yagm.api.variant;

import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

public interface IGraveVariant {

    /**
     * Unique variant identifiers (e.g. "yagm:forest" or "myaddon:cherry")
     */

    ResourceLocation getId();


    String getDisplayName();


    int getPriority();


    boolean matches(GraveVariantContext context);



    default int getTextColor() {
        return 0xFFFFFFFF;
    }


    default float getTextHeightOffset() {
        return 0f;
    }
}
