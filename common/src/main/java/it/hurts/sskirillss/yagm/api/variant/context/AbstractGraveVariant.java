package it.hurts.sskirillss.yagm.api.variant.context;

import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.resources.ResourceLocation;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@RequiredArgsConstructor
public abstract class AbstractGraveVariant implements IGraveVariant {

    @EqualsAndHashCode.Include
    protected final ResourceLocation id;
    protected final String displayName;
    protected final int priority;

    public AbstractGraveVariant(String modId, String name, String displayName, int priority) {
        this(ResourceLocation.fromNamespaceAndPath(modId, name), displayName, priority);
    }

    @Override
    public ResourceLocation getTexture(int graveLevel) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/block/grave_tier_" + graveLevel + "_" + id.getPath() + ".png");
    }

    @Override
    public ResourceLocation getModel(int graveLevel) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/grave_tier_" + graveLevel + "_" + id.getPath());
    }
}