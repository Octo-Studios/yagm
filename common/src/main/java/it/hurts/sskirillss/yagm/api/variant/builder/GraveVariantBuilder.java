package it.hurts.sskirillss.yagm.api.variant.builder;

import it.hurts.sskirillss.yagm.api.variant.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import it.hurts.sskirillss.yagm.api.variant.registry.GraveVariantRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class GraveVariantBuilder {

    private final ResourceLocation id;
    private String displayName;
    private int priority = 50;
    private final List<Predicate<GraveVariantContext>> conditions = new ArrayList<>();

    private GraveVariantBuilder(ResourceLocation id) {
        this.id = id;
    }

    public static GraveVariantBuilder create(String modId, String path) {
        return new GraveVariantBuilder(ResourceLocation.fromNamespaceAndPath(modId, path));
    }

    public static GraveVariantBuilder create(ResourceLocation id) {
        return new GraveVariantBuilder(id);
    }

    public GraveVariantBuilder displayName(String name) {
        this.displayName = name;
        return this;
    }

    public GraveVariantBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    @SafeVarargs
    public final GraveVariantBuilder matchBiomes(ResourceKey<Biome>... biomes) {
        Set<ResourceKey<Biome>> biomeSet = Set.of(biomes);
        conditions.add(ctx -> biomeSet.stream().anyMatch(ctx::isBiome));
        return this;
    }

    @SafeVarargs
    public final GraveVariantBuilder matchBiomeTags(TagKey<Biome>... tags) {
        Set<TagKey<Biome>> tagSet = Set.of(tags);
        conditions.add(ctx -> tagSet.stream().anyMatch(ctx::isBiomeTag));
        return this;
    }

    public GraveVariantBuilder inOverworldLevel() {
        conditions.add(GraveVariantContext::isOverworld);
        return this;
    }

    public GraveVariantBuilder inNetherLevel() {
        conditions.add(GraveVariantContext::isNether);
        return this;
    }

    public GraveVariantBuilder inEndLevel() {
        conditions.add(GraveVariantContext::isEnd);
        return this;
    }

    public BuiltGraveVariant build() {
        String name = Objects.requireNonNullElse(displayName, id.getPath());
        return new BuiltGraveVariant(id, name, priority, List.copyOf(conditions));
    }

    public IGraveVariant buildAndRegister() {
        IGraveVariant variant = build();
        GraveVariantRegistry.register(variant);
        return variant;
    }

    @Getter
    @AllArgsConstructor
    private static class BuiltGraveVariant implements IGraveVariant {
        private final ResourceLocation id;
        private final String displayName;
        private final int priority;
        private final List<Predicate<GraveVariantContext>> conditions;

        @Override
        public boolean matches(GraveVariantContext context) {
            if (conditions.isEmpty()) {
                return false;
            }
            return conditions.stream().allMatch(c -> c.test(context));
        }
    }
}