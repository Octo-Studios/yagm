package it.hurts.sskirillss.yagm.api.compat.fabric;

import it.hurts.sskirillss.yagm.fabric.compat.FabricCompatImpl;
import org.jetbrains.annotations.ApiStatus;

/**
 * This class delegates to FabricCompatImpl which contains the actual
 * implementation logic for Fabric-specific compatibility.
 */
@ApiStatus.Internal
public class YAGMCompatImpl {

    public static void registerPlatformHandlers() {
        FabricCompatImpl.registerPlatformHandlers();
    }

}
