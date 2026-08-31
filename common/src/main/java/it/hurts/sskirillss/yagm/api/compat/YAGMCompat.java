package it.hurts.sskirillss.yagm.api.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;

public class YAGMCompat {

    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;
        registerPlatformHandlers();
    }

    @ExpectPlatform
    public static void registerPlatformHandlers() {
        throw new AssertionError("Platform implementation missing!");
    }
}
