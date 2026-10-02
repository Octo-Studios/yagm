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

    private static volatile boolean needsSort = true;

    @Getter
    private static IGraveVariant defaultVariant;

    public static void register(IGraveVariant variant) {
        ResourceLocation id = variant.getId();

        IGraveVariant previous = GRAVESTONE_VARIANTS.putIfAbsent(id, variant);
        if (previous != null) {
            throw new IllegalArgumentException("Grave variant '" + id + "' already registered!");
        }

        needsSort = true;
    }

    public static void registerDefault(IGraveVariant variant) {
        register(variant);
        defaultVariant = variant;
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
        List<IGraveVariant> snapshot;

        synchronized (SORTED_LOADER) {
            if (needsSort) {
                SORTED_LOADER.clear();
                SORTED_LOADER.addAll(GRAVESTONE_VARIANTS.values());
                SORTED_LOADER.sort(Comparator.comparingInt(IGraveVariant::getPriority).reversed());
                needsSort = false;
            }

            snapshot = List.copyOf(SORTED_LOADER);
        }

        GraveVariantContext ctx = new GraveVariantContext(level, pos);

        IGraveVariant fallback = defaultVariant;

        for (IGraveVariant variant : snapshot) {
            if (variant != fallback && variant.matches(ctx)) {
                return variant;
            }
        }

        return fallback;
    }
}
