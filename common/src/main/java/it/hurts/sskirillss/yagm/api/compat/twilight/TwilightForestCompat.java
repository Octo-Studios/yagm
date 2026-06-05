package it.hurts.sskirillss.yagm.api.compat.twilight;

import dev.architectury.platform.Platform;
import lombok.Getter;

public final class TwilightForestCompat {

    @Getter
    private static boolean lateDeathHandlerEnabled;

    public static void init() {}

    public static void enableLateDeathHandler() {
        lateDeathHandlerEnabled = Platform.isModLoaded("twilightforest");
    }
}
