package it.hurts.sskirillss.yagm.data_components.gravestones_types;

import net.minecraft.util.StringRepresentable;

public enum GraveStoneLevels implements StringRepresentable {
    OLD_WOODEN_CROSS,
    WOODEN_CROSS,
    STONE_TOMBSTONE,
    INLAID_STONE_TOMBSTONE,
    GRANITE_TOMBSTONE;


    public static final StringRepresentable.EnumCodec<GraveStoneLevels> CODEC = StringRepresentable.fromEnum(GraveStoneLevels::values);


    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}
