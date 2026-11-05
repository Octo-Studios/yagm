package it.hurts.sskirillss.yagm.api.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import it.hurts.sskirillss.yagm.api.compat.requiem.ReliableRequiemCompat;
import it.hurts.sskirillss.yagm.api.compat.runnable.CompatRunnable;

public class YAGMCompat {

    private static boolean registered = false;

    public static void init() {
        if (registered) return;
        registered = true;
        registerPlatformHandlers();
        CompatRunnable.runIfPresent("reliable_requiem", ReliableRequiemCompat::init);
    }

    @ExpectPlatform
    public static void registerPlatformHandlers() {
        throw new AssertionError("Platform implementation missing!");
    }
}
