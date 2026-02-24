package it.hurts.sskirillss.yagm.client.particles.candle;

import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import net.minecraft.world.level.block.Block;

public final class CandleParticleInit {

    public static void init() {

         CandleParticleRegistry.register(BlockRegistry.TROPICS_GRAVESTONE_3)
                 .addCandle(11.5, 7, 5)
                 .addCandle(12, 5, 1.75)
                 .build();

         CandleParticleRegistry.register(BlockRegistry.END_GRAVESTONE_3)
                 .addCandle(0.5, 6, 2.5)
                 .build();

         CandleParticleRegistry.register(BlockRegistry.HOT_GRAVESTONE_3)
                 .addCandle(2.5, 8, 3.5)
                 .addCandle(2, 6, 1.25)
                 .build();
    }


    public static void registerSingle(RegistrySupplier<Block> block, double pixelX, double pixelY, double pixelZ) {
        CandleParticleRegistry.register(block).addCandle(pixelX, pixelY, pixelZ).build();
    }

    public static void registerSoulSingle(RegistrySupplier<net.minecraft.world.level.block.Block> block, double pixelX, double pixelY, double pixelZ) {
        CandleParticleRegistry.register(block).addSoulCandle(pixelX, pixelY, pixelZ).build();
    }
}