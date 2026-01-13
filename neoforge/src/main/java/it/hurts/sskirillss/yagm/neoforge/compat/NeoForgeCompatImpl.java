package it.hurts.sskirillss.yagm.neoforge.compat;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.CuriosCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import net.neoforged.bus.api.IEventBus;

public class NeoForgeCompatImpl {
    public static void registerPlatformHandlers() {
        if (Platform.isModLoaded("curios")) {
            AccessoryManager.registerHandler(new CuriosCompat());
        }
    }

    /**
     * Register DataComponents for Curios support.
     * Should be called during mod construction with the mod event bus.
     */
    public static void registerDataComponents(IEventBus modEventBus) {
        if (Platform.isModLoaded("curios")) {
            CurioSlotData.register(modEventBus);
        }
    }
}