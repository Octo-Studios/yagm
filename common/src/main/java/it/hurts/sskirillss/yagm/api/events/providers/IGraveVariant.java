package it.hurts.sskirillss.yagm.api.events.providers;

import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IGraveVariant {

    /**
     * Unique variant identifiers (e.g. "yagm:forest" or "myaddon:cherry")
     */

    ResourceLocation getId();


    String getDisplayName();


    int getPriority();


    boolean matches(GraveVariantContext context);


    /**
     * Optional color value for your text
     */

    default int getTextColor() {
        return 0xFFFFFFFF;
    }

    /**
     * Optional value for your text height
     */

    default float getTextHeightOffset() {
        return 0f;
    }
}
