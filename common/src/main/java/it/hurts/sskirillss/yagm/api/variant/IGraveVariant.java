package it.hurts.sskirillss.yagm.api.variant;

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

    /**
     * Returns the resource location for the emissive texture overlay.
     * Return null if this variant doesn't have an emissive texture.
     */
    default ResourceLocation getEmissiveTexture() {
        return null;
    }

    /**
     * Returns the light level for the emissive texture (0-15).
     * Default is 15 (maximum brightness).
     */
    default int getEmissiveLightLevel() {
        return 15;
    }

    /**
     * Returns an array of colors for the base glow gradient effect.
     * Each color is in 0xRRGGBB format.
     * Return null or empty array to disable the effect.
     */
    default int[] getBaseGlowColors() {
        return null;
    }
}
