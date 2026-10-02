package it.hurts.sskirillss.yagm.event;

import it.hurts.sskirillss.yagm.event.death.service.GraveDeathService;
import it.hurts.sskirillss.yagm.event.death.tracker.GraveDeathTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;
import java.util.Collections;

public class GraveStoneEvent {

    public static void handlePlayerDeath(ServerPlayer player) {
        GraveDeathService.handle(player, Collections.emptyList());
    }

    public static boolean handlePlayerDeathDrops(ServerPlayer player, Collection<ItemEntity> drops) {
        return GraveDeathService.handle(player, drops);
    }

    public static void onPlayerDeath(ServerPlayer player, CompoundTag graveData) {
        GraveDeathService.place(player, graveData);
    }

    public static void trackLastSafePositions(MinecraftServer server) {
        GraveDeathTracker.track(server);
    }

    public static void resetRuntimeState() {
        GraveDeathTracker.reset();
    }
}