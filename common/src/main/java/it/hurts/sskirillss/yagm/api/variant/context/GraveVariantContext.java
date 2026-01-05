package it.hurts.sskirillss.yagm.api.variant.context;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class GraveVariantContext {

    private final Level level;
    private final BlockPos pos;
    private final Holder<Biome> biome;
    private final ResourceKey<Level> dimension;

    public GraveVariantContext(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
        this.biome = level.getBiome(pos);
        this.dimension = level.dimension();
    }

    public int getX() { return pos.getX(); }
    public int getY() { return pos.getY(); }
    public int getZ() { return pos.getZ(); }

    public boolean isDimension(ResourceKey<Level> dim) {
        return dimension.equals(dim);
    }

    public boolean isOverworld() { return isDimension(Level.OVERWORLD); }
    public boolean isNether() { return isDimension(Level.NETHER); }
    public boolean isEnd() { return isDimension(Level.END); }

    public boolean isBiome(ResourceKey<Biome> biomeKey) {
        return biome.is(biomeKey);
    }

    public boolean isBiomeTag(TagKey<Biome> tag) {
        return biome.is(tag);
    }

    public boolean isBelow(int y) { return pos.getY() < y; }
    public boolean isAbove(int y) { return pos.getY() > y; }
    public boolean isBetween(int minY, int maxY) {
        return pos.getY() >= minY && pos.getY() <= maxY;
    }

    public BlockState getBlockBelow() {
        return level.getBlockState(pos.below());
    }

    public boolean isRaining() { return level.isRaining(); }
    public boolean isThundering() { return level.isThundering(); }
    public boolean isNight() { return level.isNight(); }
    public boolean isDay() { return level.isDay(); }
}