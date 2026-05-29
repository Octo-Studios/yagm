package it.hurts.sskirillss.yagm.api.variant;

import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface IGraveVariant {

    /**
     * Unique variant identifiers (e.g. "yagm:forest" or "myaddon:cherry")
     */

    ResourceLocation getId();


    String getDisplayName();


    /**
     * Priority replacer for id
     * !!in work!!
     * @return
     */
    int getPriority();


    boolean matches(GraveVariantContext context);

    /**
     * Candle particle positions relative to block center.
     * Each entry is {localX, localZ, yOffset}.
     * Returns null if this variant has no candles.
     */
    @Nullable
    default double[][] getCandlePositions() {
        return null;
    }
}
