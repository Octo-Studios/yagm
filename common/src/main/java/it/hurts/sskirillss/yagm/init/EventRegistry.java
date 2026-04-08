package it.hurts.sskirillss.yagm.init;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import it.hurts.sskirillss.yagm.network.handler.GhostSpawnHandler;
import lombok.extern.slf4j.Slf4j;
import it.hurts.sskirillss.yagm.event.GraveStoneEvent;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;

@Slf4j
public class EventRegistry {

    public static void init() {
        LifecycleEvent.SERVER_STARTED.register(server -> {
            CemeteryManager.getInstance().setLevelChecker(dimension -> {
                for (ServerLevel level : server.getAllLevels()) {
                    if (level.dimension().equals(dimension)) {
                        return level;
                    }
                }
                return null;
            });
            CemeteryManager.getInstance().validateAndCleanGraves();
            CemeteryManager.getInstance().reevaluateCemeteries();
        });

        LifecycleEvent.SERVER_STOPPING.register(server -> {
            GhostSpawnHandler.reset();
            GraveStoneEvent.resetRuntimeState();
        });

        TickEvent.SERVER_POST.register(GhostSpawnHandler::tick);
        TickEvent.SERVER_POST.register(GraveStoneEvent::trackLastSafePositions);


        EntityEvent.LIVING_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                if (shouldCreateGrave(player, source)) {
                    GraveStoneEvent.handlePlayerDeath(player);
                }
            }
            return EventResult.pass();
        });


        IServerEvent.ON_PLAYER_DEATH.register((player, graveData) -> {
            GraveStoneEvent.onPlayerDeath(player, graveData);
            return EventResult.pass();
        });

        IServerEvent.ON_GRAVE_PLACED.register((level, pos, state, player, graveData) -> {
            return EventResult.pass();
        });
    }

    private static boolean shouldCreateGrave(ServerPlayer player, DamageSource source) {
        return !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }
}
