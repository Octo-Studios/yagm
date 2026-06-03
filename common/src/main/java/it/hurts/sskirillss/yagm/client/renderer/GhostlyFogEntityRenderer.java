package it.hurts.sskirillss.yagm.client.renderer;

import it.hurts.sskirillss.yagm.entity.GhostlyFogEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class GhostlyFogEntityRenderer extends EntityRenderer<GhostlyFogEntity> {
    public GhostlyFogEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull GhostlyFogEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    }
}
