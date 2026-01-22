package it.hurts.sskirillss.yagm.register;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import it.hurts.sskirillss.yagm.events.GraveStoneEvent;
import it.hurts.sskirillss.yagm.api.events.providers.IServerEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameRules;

public class EventRegistry {

    public static void init() {
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