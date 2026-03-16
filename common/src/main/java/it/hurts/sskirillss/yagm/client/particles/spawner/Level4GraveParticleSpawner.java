package it.hurts.sskirillss.yagm.client.particles.spawner;

import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.entity.GraveStoneEntity;
import it.hurts.sskirillss.yagm.register.BlockRegistry;
import it.hurts.sskirillss.yagm.register.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Level4GraveParticleSpawner {

    private static final int SPAWN_CHANCE = 6;
    private static final int PARTICLES_PER_SPAWN = 1;
    private static final double MIN_SPAWN_RADIUS = 1.5D;
    private static final double MAX_SPAWN_RADIUS = 10.0D;

    public static final Map<String, ParticleOptions> CUSTOM_VARIANT_PARTICLES = new HashMap<>();
    private static final Map<Long, Long> LAST_SPAWN_TICK_BY_POS = new HashMap<>();
    static {
        // CUSTOM_VARIANT_PARTICLES.put("yagm:hot", ParticleRegistry.LEVEL4_GRAVE.get());
    }

    private static final Set<Block> LEVEL_4_BLOCKS = Set.of(
            BlockRegistry.GRAVESTONE_LEVEL_4.get(),
            BlockRegistry.COLD_GRAVESTONE_4.get(),
            BlockRegistry.HOT_GRAVESTONE_4.get(),
            BlockRegistry.NETHER_GRAVESTONE_4.get(),
            BlockRegistry.TROPICS_GRAVESTONE_4.get(),
            BlockRegistry.END_GRAVESTONE_4.get(),
            BlockRegistry.OCEAN_GRAVESTONE_4.get()
    );

    private Level4GraveParticleSpawner() {
    }

    public static void spawn(Level level, GraveStoneEntity entity, BlockState state, RandomSource rand) {
        if (level == null || !level.isClientSide || entity == null || state == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) return;
        if (!minecraft.getWindow().isFullscreen() || minecraft.getWindow().shouldClose()) return;

        if (!isLevel4Block(state)) return;

        BlockPos pos = entity.getBoundPos();
        long gameTime = level.getGameTime();
        long posKey = pos.asLong();
        Long lastTick = LAST_SPAWN_TICK_BY_POS.put(posKey, gameTime);

        if (lastTick != null && lastTick == gameTime) return;
        if (LAST_SPAWN_TICK_BY_POS.size() > 4096) {
            LAST_SPAWN_TICK_BY_POS.clear();
        }

        if (rand.nextInt(SPAWN_CHANCE) != 0) return;
        double centerX = pos.getX() + 0.5D;
        double centerZ = pos.getZ() + 0.5D;

        ParticleOptions particle = ParticleRegistry.LEVEL4_GRAVE.get();
        if (entity.getVariant() != null && entity.getVariant().getId() != null) {
            String id = entity.getVariant().getId().toString().toLowerCase(Locale.ROOT);
            ParticleOptions custom = CUSTOM_VARIANT_PARTICLES.get(id);
            if (custom != null) particle = custom;
        }

        for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double radiusBiasToCenter = Math.pow(rand.nextDouble(), 1.2D);
            double radius = MIN_SPAWN_RADIUS + radiusBiasToCenter * (MAX_SPAWN_RADIUS - MIN_SPAWN_RADIUS);

            double spawnX = centerX + Math.cos(angle) * radius;
            double spawnY = pos.getY() - 0.8D - rand.nextDouble() * 0.6D;
            double spawnZ = centerZ + Math.sin(angle) * radius;

            double vx = (rand.nextDouble() - 0.5D) * 0.0012D;
            double vy = 0.028D + rand.nextDouble() * 0.01D;
            double vz = (rand.nextDouble() - 0.5D) * 0.0012D;

            level.addParticle(particle, spawnX, spawnY, spawnZ, vx, vy, vz);
        }
    }

    private static boolean isLevel4Block(BlockState state) {
        return LEVEL_4_BLOCKS.contains(state.getBlock());
    }
}
