package it.hurts.sskirillss.yagm.event.death.tracker;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GraveDeathTracker {

    private static final Set<UUID> ASL_SET = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Map<UUID, Long> DEATH_TICK = new ConcurrentHashMap<>();

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
    }
}
