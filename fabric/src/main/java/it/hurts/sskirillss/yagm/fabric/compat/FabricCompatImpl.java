package it.hurts.sskirillss.yagm.fabric.compat;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.fabric.compat.accessories.AccessoriesCompat;
import it.hurts.sskirillss.yagm.fabric.compat.trinkets.TrinketsCompat;

public class FabricCompatImpl {
    public static void registerPlatformHandlers() {
        if (Platform.isModLoaded("accessories")) {
            AccessoryLoader.registerHandler(new AccessoriesCompat());
        } else if (Platform.isModLoaded("trinkets")) {
            AccessoryLoader.registerHandler(new TrinketsCompat());
        }
    }
}
