package it.hurts.sskirillss.yagm.api.compat;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.YAGMCommon;

public class YAGMCompat {

    private static boolean initialized = false;

    private YAGMCompat() {}

    /**
     * Initialize all mod compatibility.
     * Should be called from mod entrypoint after YAGMCommon.init()
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        YAGMCommon.LOGGER.info("[YAGM] Initializing mod compatibility...");

        registerPlatformHandlers();

        AccessoryManager.initialize();

        YAGMCommon.LOGGER.info("[YAGM] Mod compatibility initialized. {} accessory handler(s) registered.", AccessoryManager.getHandlers().size());
    }

    /**
     * Platform-specific handler registration.
     * Implemented in neoforge/fabric modules as CompatInitImpl.
     */
    @ExpectPlatform
    public static void registerPlatformHandlers() {
        throw new AssertionError("Platform implementation missing!");
    }

    /**
     * @return true if any accessory mod is loaded
     */
    public static boolean hasAccessoryMod() {
        return Platform.isModLoaded("curios") || Platform.isModLoaded("trinkets");
    }
}