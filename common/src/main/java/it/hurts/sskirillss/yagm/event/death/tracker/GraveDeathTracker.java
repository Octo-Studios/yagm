package it.hurts.sskirillss.yagm.event.death.tracker;

import it.hurts.sskirillss.yagm.component.placement.GravePositionResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GraveDeathTracker {

    private static final Set<UUID> ASL_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Long> DEATH_TICK = new ConcurrentHashMap<>();
    private static final Map<UUID, SafePos> MAP_SAFE = new ConcurrentHashMap<>();

    private static volatile boolean POST_DEATH;

    public static boolean begin(ServerPlayer player) {
        UUID uuid = player.getUUID();
        long tick = player.serverLevel().getGameTime();

        Long previousTick = DEATH_TICK.get(uuid);

        if (previousTick != null && tick >= previousTick && tick - previousTick <= 2L) {
            return false;
        }

        return ASL_SET.add(uuid);
    }

    public static void getdeathhandler() {
        POST_DEATH = true;
    }

    public static boolean isPostDeath() {
        return POST_DEATH;
    }

    public static void set(ServerPlayer player) {
        DEATH_TICK.put(player.getUUID(), player.serverLevel().getGameTime());
    }

    public static void end(ServerPlayer player) {
        ASL_SET.remove(player.getUUID());
    }

    public static void reset() {
        ASL_SET.clear();
        DEATH_TICK.clear();
        MAP_SAFE.clear();
    }

    public static void track(MinecraftServer server){
        for(ServerPlayer player : server.getPlayerList().getPlayers()){
            updatepos(player);
        }

        long tickable = server.overworld().getGameTime();

        DEATH_TICK.entrySet().removeIf(entry -> tickable - entry.getValue() > 20L);
        MAP_SAFE.keySet().removeIf(uuid -> server.getPlayerList().getPlayer(uuid) == null);
    }

    private static void updatepos(ServerPlayer player) {
        if (!GravePositionResolver.canTrackSafeStand(player)) {
            return;
        }

        MAP_SAFE.put(player.getUUID(), new SafePos(player.level().dimension(), player.position()));
    }

    public static BlockPos trackedPlacementPos(ServerLevel level, ServerPlayer player) {
        SafePos tracked = MAP_SAFE.get(player.getUUID());

        if (tracked != null && tracked.dimension().equals(level.dimension())) {
            return BlockPos.containing(tracked.position());
        }

        return null;
    }

    public static Vec3 trackedSpawnPos(ServerLevel level, ServerPlayer player) {
        SafePos tracked = MAP_SAFE.get(player.getUUID());

        if (tracked != null && tracked.dimension().equals(level.dimension())) {
            return tracked.position();
        }

        return null;
    }

    private record SafePos(ResourceKey<Level> dimension, Vec3 position) {}
}
