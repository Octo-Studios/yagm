package it.hurts.sskirillss.yagm.client.particles.candle;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.level.block.Block;

import java.util.*;

public final class CandleParticleRegistry {

    private static final Map<Block, List<CandlePosition>> CANDLE_POSITIONS = new HashMap<>();

    public static Builder register(RegistrySupplier<Block> blockSupplier) {
        return new Builder(blockSupplier);
    }


    public static Builder register(Block block) {
        return new Builder(block);
    }

    public static List<CandlePosition> getCandlePositions(Block block) {
        return CANDLE_POSITIONS.getOrDefault(block, Collections.emptyList());
    }


    public static boolean hasCandles(Block block) {
        return CANDLE_POSITIONS.containsKey(block);
    }


    public static Set<Block> getRegisteredBlocks() {
        return Collections.unmodifiableSet(CANDLE_POSITIONS.keySet());
    }


    public static void clear() {
        CANDLE_POSITIONS.clear();
    }

    public static class Builder {
        private final Object blockSource;
        private final List<CandlePosition> positions = new ArrayList<>();

        private Builder(RegistrySupplier<Block> blockSupplier) {
            this.blockSource = blockSupplier;
        }

        private Builder(Block block) {
            this.blockSource = block;
        }

        public Builder addCandle(CandlePosition position) {
            positions.add(position);
            return this;
        }


        public Builder addCandle(double pixelX, double pixelY, double pixelZ) {
            positions.add(CandlePosition.builder().at(pixelX, pixelY, pixelZ).build());
            return this;
        }


        public Builder addCandle(double pixelX, double pixelY, double pixelZ, double yOffset) {
            positions.add(CandlePosition.builder().at(pixelX, pixelY, pixelZ).offsetY(yOffset).build());
            return this;
        }


        public Builder addSoulCandle(double pixelX, double pixelY, double pixelZ) {
            positions.add(CandlePosition.builder().at(pixelX, pixelY, pixelZ).soulFire().build());
            return this;
        }


        public Builder addSoulCandle(double pixelX, double pixelY, double pixelZ, double yOffset) {
            positions.add(CandlePosition.builder().at(pixelX, pixelY, pixelZ).offsetY(yOffset).soulFire().build());
            return this;
        }


        public void build() {
            Block block;
            if (blockSource instanceof RegistrySupplier<?> supplier) {
                block = (Block) supplier.get();
            } else {
                block = (Block) blockSource;
            }

            if (!positions.isEmpty()) {
                CANDLE_POSITIONS.put(block, new ArrayList<>(positions));
            }
        }
    }
}