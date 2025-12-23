package it.hurts.sskirillss.yagm.register;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;


public final class BlockRegistry {

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(YAGMCommon.MODID, Registries.BLOCK);

    public static final RegistrySupplier<Block> GRAVE_STONE = BLOCKS.register("grave_stone", () -> new GraveStoneBlock(
         BlockBehaviour.Properties.of().strength(0.1F, 6.0F).requiresCorrectToolForDrops()));


    public static void init() {
        BLOCKS.register();
    }
}
