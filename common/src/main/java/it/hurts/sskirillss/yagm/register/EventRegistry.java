package it.hurts.sskirillss.yagm.register;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.item_valuator.ItemValuator;
import it.hurts.sskirillss.yagm.events.GraveStoneEvent;
import it.hurts.sskirillss.yagm.api.events.providers.IServerEvent;
import it.hurts.sskirillss.yagm.structures.cemetery.CemeteryManager;
import it.hurts.sskirillss.yagm.test.CemeteryTestLogger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;

public class EventRegistry {

    public static void init() {
        LifecycleEvent.SERVER_STARTED.register(server -> {
            ItemValuator.initialize(server);
            YAGMCommon.LOGGER.info("ItemValuator initialized");
            CemeteryTestLogger.init(server);
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
        LifecycleEvent.SERVER_STOPPING.register(server -> ItemValuator.shutdown());


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