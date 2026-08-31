package it.hurts.sskirillss.yagm.api.compat.twilight;

import dev.architectury.platform.Platform;
import lombok.Getter;

public final class TwilightForestCompat {

    @Getter
    private static boolean POST_DEATH;

    public static void init() {}

    public static void setdeathHandler() {
        POST_DEATH = Platform.isModLoaded("twilightforest");
    }
}
