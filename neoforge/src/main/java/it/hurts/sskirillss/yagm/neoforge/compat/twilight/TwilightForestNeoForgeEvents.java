package it.hurts.sskirillss.yagm.neoforge.compat.twilight;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.twilight.TwilightForestCompat;
import it.hurts.sskirillss.yagm.event.GraveStoneEvent;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public final class TwilightForestNeoForgeEvents {
    private static boolean initialized;

    public static void register() {
        if (initialized || !Platform.isModLoaded("twilightforest")) {
            return;
        }

        initialized = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TwilightForestNeoForgeEvents::onLivingDeath);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || player instanceof FakePlayer
                || player.isSpectator()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                || TwilightForestCompat.shouldSuppressGraveAfterTwilight(player)
                || !InventoryUtils.hasRecoverableItems(player)) {
            return;
        }

        GraveStoneEvent.handlePlayerDeath(player);
    }
}
