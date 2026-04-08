package it.hurts.sskirillss.yagm.component.ghost_mode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@AllArgsConstructor
@SuppressWarnings("all")
public enum BehaviorMode implements StringRepresentable {

    FOLLOW("yagm.ghost.mode.follow"),
    WANDER("yagm.ghost.mode.wander"),
    STAY("yagm.ghost.mode.stay");

    public static final StringRepresentable.EnumCodec<BehaviorMode> CODEC = StringRepresentable.fromEnum(BehaviorMode::values);

    @Getter
    private final String translationKey;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    public BehaviorMode next() {
        BehaviorMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
