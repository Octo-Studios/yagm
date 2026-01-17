package it.hurts.sskirillss.yagm.api.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class YAGMCompat {

    private static boolean initialized = false;

    private YAGMCompat() {}


    public static void init() {
        if (initialized) return;
        initialized = true;
        registerPlatformHandlers();
        AccessoryManager.initialize();
    }

    /**
     * Implemented in neoforge/fabric modules as CompatInitImpl.
     */
    @ExpectPlatform
    public static void registerPlatformHandlers() {
        throw new AssertionError("Platform implementation missing!");
    }


    public static boolean hasAccessoryMod() {
        return Platform.isModLoaded("curios") || Platform.isModLoaded("trinkets");
    }
}
