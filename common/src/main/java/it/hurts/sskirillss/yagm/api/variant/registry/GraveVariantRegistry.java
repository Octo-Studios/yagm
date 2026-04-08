package it.hurts.sskirillss.yagm.api.variant.registry;

import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class GraveVariantRegistry {

    private static final Map<ResourceLocation, IGraveVariant> GRAVESTONE_VARIANTS = new ConcurrentHashMap<>();
    private static final List<IGraveVariant> SORTED_LOADER = new ArrayList<>();
    public static boolean valid_sort = true;

    @Getter
    private static IGraveVariant defaultVariant;

    public static void register(IGraveVariant variant) {
        ResourceLocation id = variant.getId();

        if (GRAVESTONE_VARIANTS.containsKey(id)) {
            throw new IllegalArgumentException("Grave variant '" + id + "' already registered!");
        }

        GRAVESTONE_VARIANTS.put(id, variant);
        valid_sort = true;
    }

    @Nullable
    public static IGraveVariant get(ResourceLocation id) {
        return GRAVESTONE_VARIANTS.get(id);
    }

    @Nullable
    public static IGraveVariant get(String id) {
        return get(ResourceLocation.tryParse(id));
    }


    public static IGraveVariant getFor(Level level, BlockPos pos) {
        if (valid_sort) {
            synchronized (SORTED_LOADER) {
                SORTED_LOADER.clear();
                SORTED_LOADER.addAll(GRAVESTONE_VARIANTS.values());
                SORTED_LOADER.sort(Comparator.comparingInt(IGraveVariant::getPriority).reversed());
                valid_sort = false;
            }
        }

        GraveVariantContext ctx = new GraveVariantContext(level, pos);
        for (IGraveVariant variant : SORTED_LOADER) {
            if (variant != defaultVariant && variant.matches(ctx)) {
                return variant;
            }
        }

        return defaultVariant;
    }
}
