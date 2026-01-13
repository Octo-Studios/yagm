package it.hurts.sskirillss.yagm.api.compat.neoforge;


import it.hurts.sskirillss.yagm.neoforge.compat.NeoForgeCompatImpl;

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
        NeoForgeCompatImpl.registerPlatformHandlers();
    }

}