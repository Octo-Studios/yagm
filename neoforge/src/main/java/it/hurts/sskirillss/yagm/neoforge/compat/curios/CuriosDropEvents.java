package it.hurts.sskirillss.yagm.neoforge.compat.curios;

import it.hurts.sskirillss.yagm.neoforge.event.NeoForgeDropTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.event.CurioDropsEvent;

public final class CuriosDropEvents {

    private static boolean initialized;

    public static void register() {
        if (initialized) {
            return;
        }

        initialized = true;
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, CuriosDropEvents::onCurioDrops);
    }

    private static void onCurioDrops(CurioDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            NeoForgeDropTracker.trackAccessoryDrops(player.getUUID(), event.getDrops());
        }
    }
}
