package it.hurts.sskirillss.yagm.api.variant.context;

import it.hurts.sskirillss.yagm.api.events.providers.IGraveVariant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
}
