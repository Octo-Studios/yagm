package it.hurts.sskirillss.yagm.client.particles.spawner;

import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.block.GraveStoneBlock;
import it.hurts.sskirillss.yagm.client.particles.candle.CandleParticleRegistry;
import it.hurts.sskirillss.yagm.client.particles.candle.CandlePosition;
import it.hurts.sskirillss.yagm.register.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class CandleFlameSpawner {

    private static final boolean USE_TORCH_FLAME = true;

    private static final Map<Long, Long> LAST_SPAWN_TICK = new HashMap<>();
    private static final int RESPAWN_DELAY_TICKS = 35;

    private CandleFlameSpawner() {
    }

    public static void spawn(Level level, BlockPos pos, BlockState state) {
        if (level == null || !level.isClientSide() || state == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }

        Block block = state.getBlock();
        List<CandlePosition> candles = CandleParticleRegistry.getCandlePositions(block);

        if (candles.isEmpty()) {
            return;
        }

        long gameTime = level.getGameTime();
        long posKey = pos.asLong();

        Long lastSpawn = LAST_SPAWN_TICK.get(posKey);
        if (lastSpawn != null && gameTime - lastSpawn < RESPAWN_DELAY_TICKS) {
            return;
        }

        LAST_SPAWN_TICK.put(posKey, gameTime);

        if (LAST_SPAWN_TICK.size() > 2048) {
            LAST_SPAWN_TICK.clear();
        }

        Direction facing = Direction.NORTH;
        if (state.hasProperty(GraveStoneBlock.FACING)) {
            facing = state.getValue(GraveStoneBlock.FACING);
        }

        for (CandlePosition candle : candles) {
            spawnFlame(level, pos, candle, facing);
        }
    }

    private static void spawnFlame(Level level, BlockPos pos, CandlePosition candle, Direction facing) {
        CandlePosition rotated = candle.rotateForFacing(facing);

        double x = pos.getX() + rotated.getBlockX();
        double y = pos.getY() + rotated.getBlockY();
        double z = pos.getZ() + rotated.getBlockZ();

        ParticleOptions particle;
        if (USE_TORCH_FLAME) {
            particle = candle.isSoulFire() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        } else {
            particle = candle.isSoulFire()
                    ? ParticleRegistry.SOUL_CANDLE_FLAME.get()
                    : ParticleRegistry.CANDLE_FLAME.get();
        }

        level.addParticle(particle, x, y, z, 0, 0, 0);
    }

    public static void clearCache() {
        LAST_SPAWN_TICK.clear();
    }
}