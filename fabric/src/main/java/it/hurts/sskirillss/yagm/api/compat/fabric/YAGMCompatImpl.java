package it.hurts.sskirillss.yagm.api.compat.fabric;

import it.hurts.sskirillss.yagm.fabric.compat.FabricCompatImpl;

/**
 * Platform-specific implementation for YAGMCompat.registerPlatformHandlers()
 * on Fabric platform.
 *
 * This class delegates to FabricCompatImpl which contains the actual
 * implementation logic for Fabric-specific compatibility.
 */
public class YAGMCompatImpl {

    /**
     * Register platform-specific accessory handlers for Fabric.
     * This is the implementation for YAGMCompat.registerPlatformHandlers()
     * Delegates to FabricCompatImpl for actual logic.
     */
    public static void registerPlatformHandlers() {
        FabricCompatImpl.registerPlatformHandlers();
    }

}