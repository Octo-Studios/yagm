package it.hurts.sskirillss.yagm.api.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
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

    @ExpectPlatform
    public static void registerPlatformHandlers() {
        throw new AssertionError("Platform implementation missing!");
    }
}
