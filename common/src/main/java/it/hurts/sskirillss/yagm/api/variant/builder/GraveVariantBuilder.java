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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class GraveVariantBuilder {

    private final ResourceLocation id;
    private String displayName;
    private int priority = 50;
    private final List<Predicate<GraveVariantContext>> conditions = new ArrayList<>();
    private int textColor = 0xFFFFFFFF;
    private float textHeightOffset = 0f;
    private String texturePattern;
    private String modelPattern;

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

    public GraveVariantBuilder matchBiome(ResourceKey<Biome> biome) {
        conditions.add(ctx -> ctx.isBiome(biome));
        return this;
    }

    public GraveVariantBuilder matchBiomeTag(TagKey<Biome> tag) {
        conditions.add(ctx -> ctx.isBiomeTag(tag));
        return this;
    }

    public GraveVariantBuilder matchDimension(ResourceKey<Level> dimension) {
        conditions.add(ctx -> ctx.isDimension(dimension));
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

    public GraveVariantBuilder getIsMatch(Predicate<GraveVariantContext> condition) {
        conditions.add(condition);
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

    public GraveVariantBuilder texturePattern(String pattern) {
        this.texturePattern = pattern;
        return this;
    }

    public GraveVariantBuilder modelPattern(String pattern) {
        this.modelPattern = pattern;
        return this;
    }

    public IGraveVariant build() {
        String name = displayName != null ? displayName : id.getPath();

        return new BuiltGraveVariant(
                id, name, priority,
                List.copyOf(conditions),
                textColor, textHeightOffset,
                texturePattern, modelPattern
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
        private final String texturePattern;
        private final String modelPattern;

        BuiltGraveVariant(ResourceLocation id, String displayName, int priority, List<Predicate<GraveVariantContext>> conditions, int textColor, float textHeightOffset, String texturePattern, String modelPattern) {
            super(id, displayName, priority);
            this.conditions = conditions;
            this.textColor = textColor;
            this.textHeightOffset = textHeightOffset;
            this.texturePattern = texturePattern;
            this.modelPattern = modelPattern;
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

        @Override
        public ResourceLocation getTexture(int graveLevel) {
            if (texturePattern != null) {
                return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), texturePattern.replace("{level}", String.valueOf(graveLevel)));
            }
            return super.getTexture(graveLevel);
        }

        @Override
        public ResourceLocation getModel(int graveLevel) {
            if (modelPattern != null) {
                return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), modelPattern.replace("{level}", String.valueOf(graveLevel)));
            }
            return super.getModel(graveLevel);
        }
    }
}