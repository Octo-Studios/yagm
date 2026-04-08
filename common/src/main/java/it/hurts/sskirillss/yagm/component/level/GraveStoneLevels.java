package it.hurts.sskirillss.yagm.component.level;

import lombok.Getter;
import net.minecraft.util.StringRepresentable;

@Getter
@SuppressWarnings("all")
public enum GraveStoneLevels implements StringRepresentable {

    GRAVESTONE_LEVEL_1,
    GRAVESTONE_LEVEL_2,
    GRAVESTONE_LEVEL_3,
    GRAVESTONE_LEVEL_4;

    public static final StringRepresentable.EnumCodec<GraveStoneLevels> CODEC = StringRepresentable.fromEnum(GraveStoneLevels::values);

    public int getLevel() {
        return ordinal() + 1;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

}