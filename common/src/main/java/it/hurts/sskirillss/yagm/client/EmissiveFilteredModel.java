package it.hurts.sskirillss.yagm.client;

import lombok.AllArgsConstructor;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a {@link BakedModel} and filters quads by whether their sprite is an emissive texture.
 *
 * <ul>
 *   <li>{@code emissiveOnly = true}  — returns only quads whose sprite path contains "emissive"</li>
 *   <li>{@code emissiveOnly = false} — returns all other quads</li>
 * </ul>
 */
@AllArgsConstructor
public class EmissiveFilteredModel implements BakedModel {

    private final BakedModel wrapped;
    private final boolean emissiveOnly;

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction direction, RandomSource random) {
        List<BakedQuad> original = wrapped.getQuads(state, direction, random);
        List<BakedQuad> filtered = new ArrayList<>(original.size());

        for (BakedQuad quad : original) {
            boolean isEmissive = quad.getSprite().contents().name().getPath().contains("emissive");
            if (isEmissive == emissiveOnly) {
                filtered.add(quad);
            }
        }

        return filtered;
    }

    @Override public boolean useAmbientOcclusion() { return wrapped.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return wrapped.isGui3d(); }
    @Override public boolean usesBlockLight() { return wrapped.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return wrapped.isCustomRenderer(); }
    @Override public TextureAtlasSprite getParticleIcon() { return wrapped.getParticleIcon(); }
    @Override public ItemTransforms getTransforms() { return wrapped.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return wrapped.getOverrides(); }
}
