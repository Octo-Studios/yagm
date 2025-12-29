package it.hurts.sskirillss.yagm.data_components.gravestones_types;


import lombok.Getter;
import net.minecraft.util.StringRepresentable;

import java.util.EnumSet;
import java.util.Set;


@Getter
public enum GraveStoneLevels implements StringRepresentable {

    GRAVESTONE_LEVEL_1(EnumSet.noneOf(GraveFeature.class)),
    GRAVESTONE_LEVEL_2(EnumSet.of(GraveFeature.OWNER_NAME)),
    GRAVESTONE_LEVEL_3(EnumSet.of(GraveFeature.OWNER_NAME, GraveFeature.DEATH_TIME)),
    GRAVESTONE_LEVEL_4(EnumSet.of(GraveFeature.OWNER_NAME, GraveFeature.DEATH_TIME, GraveFeature.DEATH_CAUSE)),
    GRAVESTONE_LEVEL_5(EnumSet.allOf(GraveFeature.class));


    public static final StringRepresentable.EnumCodec<GraveStoneLevels> CODEC = StringRepresentable.fromEnum(GraveStoneLevels::values);

    private final Set<GraveFeature> features;

    GraveStoneLevels(Set<GraveFeature> features) {
        this.features = features;
    }

    public boolean hasFeature(GraveFeature feature) {
        return features.contains(feature);
    }

    public int getLevel() {
        return ordinal() + 1;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }

    public enum GraveFeature {
        OWNER_NAME,
        DEATH_TIME,
        DEATH_CAUSE,
        TESTAMENT
    }
}