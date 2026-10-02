package it.hurts.sskirillss.yagm.api.compat.runnable;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.runnable.hook.CompatHook;

import java.util.Objects;

public final class CompatRunnable {

    public static CompatHook getModId(String modId, Runnable action) {
        return new CompatHook(modId, action);
    }

    public static boolean runFirst(CompatHook... hooks) {
        Objects.requireNonNull(hooks, "hooks");

        for (CompatHook hook : hooks) {
            if (CompatRunnable.runIfPresent(hook)) {
                return true;
            }
        }

        return false;
    }

    public static void run(CompatHook... hooks) {
        Objects.requireNonNull(hooks, "hooks");

        for (CompatHook hook : hooks) {
            CompatRunnable.runIfPresent(hook);
        }
    }

    public static boolean runIfPresent(CompatHook hook) {
        Objects.requireNonNull(hook, "hook");

        return CompatRunnable.runIfPresent(hook.modId(), hook.action());
    }

    public static boolean runIfPresent(String modId, Runnable action) {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(action, "action");

        if (!Platform.isModLoaded(modId)) {
            return false;
        }

        action.run();

        return true;
    }
}
