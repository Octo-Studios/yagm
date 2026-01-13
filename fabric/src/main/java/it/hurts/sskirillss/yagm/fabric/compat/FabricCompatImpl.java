package it.hurts.sskirillss.yagm.fabric.compat;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.AccessoryManager;
import it.hurts.sskirillss.yagm.fabric.compat.trinkets.TrinketsCompat;

public class FabricCompatImpl {
    public static void registerPlatformHandlers() {
        if (Platform.isModLoaded("trinkets")) {
            AccessoryManager.registerHandler(new TrinketsCompat());
        }
    }
}
