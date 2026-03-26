package it.hurts.sskirillss.yagm.component.ghost_mode;

import lombok.Getter;

public enum BehaviorMode {
    FOLLOW("yagm.ghost.mode.follow"),
    WANDER("yagm.ghost.mode.wander"),
    STAY("yagm.ghost.mode.stay");

    @Getter
    private final String translationKey;

    BehaviorMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public BehaviorMode next() {
        BehaviorMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}