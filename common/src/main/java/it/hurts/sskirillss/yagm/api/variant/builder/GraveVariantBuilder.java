package it.hurts.sskirillss.yagm.api.variant.builder;

import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import it.hurts.sskirillss.yagm.api.variant.context.GraveVariantContext;
import it.hurts.sskirillss.yagm.api.variant.context.registry.GraveVariantRegistry;
import it.hurts.sskirillss.yagm.api.variant.context.AbstractGraveVariant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

@ApiStatus.Internal
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GraveVariantBuilder {

    private final ResourceLocation id;
    private String displayName;
    private int priority = 50;
    private final List<Predicate<GraveVariantContext>> conditions = new ArrayList<>();
    private int textColor = 0xFFFFFFFF;
    private float textHeightOffset = 0f;

    public static GraveVariantBuilder create(ResourceLocation id) {
        return new GraveVariantBuilder(id);
    }

    public static GraveVariantBuilder create(String modId, String name) {
        return new GraveVariantBuilder(ResourceLocation.fromNamespaceAndPath(modId, name));
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

    @SafeVarargs
    public final GraveVariantBuilder matchDimensions(ResourceKey<Level>... dimensions) {
        Set<ResourceKey<Level>> dimensionSet = Set.of(dimensions);
        conditions.add(ctx -> dimensionSet.stream().anyMatch(ctx::isDimension));
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

    public GraveVariantBuilder belowY(int y) {
        conditions.add(ctx -> ctx.isBelow(y));
        return this;
    }

    public GraveVariantBuilder aboveY(int y) {
        conditions.add(ctx -> ctx.isAbove(y));
        return this;
    }

    public GraveVariantBuilder betweenY(int minY, int maxY) {
        conditions.add(ctx -> ctx.isBetween(minY, maxY));
        return this;
    }


    public GraveVariantBuilder textColor(int color) {
        this.textColor = color;
        return this;
    }

    public GraveVariantBuilder textHeightOffset(float offset) {
        this.textHeightOffset = offset;
        return this;
    }

    public IGraveVariant build() {
        String name = displayName != null ? displayName : id.getPath();

        return new BuiltGraveVariant(
                id, name, priority,
                List.copyOf(conditions),
                textColor, textHeightOffset
        );
    }

    public IGraveVariant buildAndRegister() {
        IGraveVariant variant = build();
        GraveVariantRegistry.register(variant);
        return variant;
    }


    private static class BuiltGraveVariant extends AbstractGraveVariant {

        private final List<Predicate<GraveVariantContext>> conditions;
        private final int textColor;
        private final float textHeightOffset;

        BuiltGraveVariant(ResourceLocation id, String displayName, int priority, List<Predicate<GraveVariantContext>> conditions, int textColor, float textHeightOffset) {
            super(id, displayName, priority);
            this.conditions = conditions;
            this.textColor = textColor;
            this.textHeightOffset = textHeightOffset;
        }

        @Override
        public boolean matches(GraveVariantContext context) {
            if (conditions.isEmpty()) return false;
            return conditions.stream().allMatch(c -> c.test(context));
        }

        @Override
        public int getTextColor() {
            return textColor;
        }

        @Override
        public float getTextHeightOffset() {
            return textHeightOffset;
        }
    }
}
