package it.hurts.sskirillss.yagm.component.ghost_mode;

import lombok.Getter;

public enum GhostMood {
    DEFAULT("ghost_default"),
    ANGRY("ghost_angry"),
    HAPPY("ghost_happy"),
    SHY("ghost_shy"),
    SAD("ghost_sad"),
    NEUTRAL("ghost_neutral");

    @Getter
    private final String textureName;

    GhostMood(String textureName) {
        this.textureName = textureName;
    }

    public static GhostMood fromString(String name) {
        for (GhostMood mood : values()) {
            if (mood.textureName.equals(name)) return mood;
        }
        return DEFAULT;
    }
}