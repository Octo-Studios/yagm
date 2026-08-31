package it.hurts.sskirillss.yagm.neoforge.event;

import it.hurts.sskirillss.yagm.api.compat.twilight.TwilightForestCompat;
import it.hurts.sskirillss.yagm.event.GraveStoneEvent;
import it.hurts.sskirillss.yagm.event.death.tracker.GraveDeathTracker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class NeoForgeDeathEvents {

    private static boolean initialized;

    public static void register() {
        if (initialized) return;

        initialized = true;
        GraveDeathTracker.getdeathhandler();

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeoForgeDeathEvents::onLivingDrops);
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        if (TwilightForestCompat.isPOST_DEATH()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || player instanceof FakePlayer
                || player.isSpectator()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        if (GraveStoneEvent.handlePlayerDeathDrops(player, event.getDrops())) {
            event.getDrops().clear();
        }
    }
}