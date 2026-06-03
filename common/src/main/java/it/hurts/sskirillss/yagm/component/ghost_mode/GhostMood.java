package it.hurts.sskirillss.yagm.component.ghost_mode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@AllArgsConstructor
@SuppressWarnings("all")
public enum GhostMood implements StringRepresentable {

    DEFAULT("ghost_default"),
    ANGRY("ghost_angry"),
    HAPPY("ghost_happy"),
    SHY("ghost_shy"),
    SAD("ghost_sad"),
    NEUTRAL("ghost_neutral");

    public static final StringRepresentable.EnumCodec<GhostMood> CODEC = StringRepresentable.fromEnum(GhostMood::values);

    @Getter
    private final String textureName;

    @Override
    public String getSerializedName() {
        return textureName;
    }

    public static GhostMood fromString(String name) {
        return CODEC.byName(name, DEFAULT);
    }
}
