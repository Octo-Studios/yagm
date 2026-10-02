package it.hurts.sskirillss.yagm.api.compat.runnable.hook;

import java.util.Objects;

public record CompatHook(String modId, Runnable action) {

    public CompatHook {
        Objects.requireNonNull(modId, "modId");
        Objects.requireNonNull(action, "action");
    }
}