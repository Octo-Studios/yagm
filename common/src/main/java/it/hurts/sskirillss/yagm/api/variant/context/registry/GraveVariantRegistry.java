package it.hurts.sskirillss.yagm.api.variant.context.registry;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GraveVariantRegistry {
    private static final Map<ResourceLocation, IGraveVariant> GRAVESTONE_VARIANTS = new ConcurrentHashMap<>();
    private static final List<IGraveVariant> SORTED_VARIANTS = new ArrayList<>();
    public static boolean needsSort = true;


    @Getter
    private static IGraveVariant defaultVariant;

    private GraveVariantRegistry() {}

    public static void register(IGraveVariant variant) {
        ResourceLocation id = variant.getId();

        if (GRAVESTONE_VARIANTS.containsKey(id)) {
            throw new IllegalArgumentException("Grave variant '" + id + "' already registered!");
        }

        GRAVESTONE_VARIANTS.put(id, variant);
        needsSort = true;

        YAGMCommon.LOGGER.info("Registered grave variant: {}", id);
    }

    public static void setDefaultVariant(IGraveVariant variant) {
        defaultVariant = variant;
        register(variant);
    }

    @Nullable
    public static IGraveVariant get(ResourceLocation id) {
        return GRAVESTONE_VARIANTS.get(id);
    }

    @Nullable
    public static IGraveVariant get(String id) {
        return get(ResourceLocation.tryParse(id));
    }

    public static Collection<IGraveVariant> getAll() {
        return Collections.unmodifiableCollection(GRAVESTONE_VARIANTS.values());
    }

    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(GRAVESTONE_VARIANTS.keySet());
    }

    public static IGraveVariant getFor(Level level, BlockPos pos) {
        ensureSorted();

        GraveVariantContext ctx = new GraveVariantContext(level, pos);

        for (IGraveVariant variant : SORTED_VARIANTS) {
            if (variant != defaultVariant && variant.matches(ctx)) {
                return variant;
            }
        }

        return defaultVariant;
    }

    public static IGraveVariant getFor(Level level, int x, int y, int z) {
        return getFor(level, new BlockPos(x, y, z));
    }

    public static boolean isRegistered(ResourceLocation id) {
        return GRAVESTONE_VARIANTS.containsKey(id);
    }

    public static int count() {
        return GRAVESTONE_VARIANTS.size();
    }

    private static void ensureSorted() {
        if (needsSort) {
            synchronized (SORTED_VARIANTS) {
                SORTED_VARIANTS.clear();
                SORTED_VARIANTS.addAll(GRAVESTONE_VARIANTS.values());
                SORTED_VARIANTS.sort(Comparator.comparingInt(IGraveVariant::getPriority).reversed());
                needsSort = false;
            }
        }
    }
}
