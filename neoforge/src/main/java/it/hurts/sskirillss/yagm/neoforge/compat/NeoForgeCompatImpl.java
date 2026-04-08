package it.hurts.sskirillss.yagm.neoforge.compat;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.neoforge.compat.accessories.AccessoriesCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.CuriosCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import net.neoforged.bus.api.IEventBus;

public class NeoForgeCompatImpl {
    public static void registerPlatformHandlers() {
        if (Platform.isModLoaded("accessories")) {
            AccessoryLoader.registerHandler(new AccessoriesCompat());
        }
        if (Platform.isModLoaded("curios")) {
            AccessoryLoader.registerHandler(new CuriosCompat());
        }
    }

    public static void registerDataComponents(IEventBus modEventBus) {
        if (Platform.isModLoaded("curios")) {
            CurioSlotData.register(modEventBus);
        }
    }
}
