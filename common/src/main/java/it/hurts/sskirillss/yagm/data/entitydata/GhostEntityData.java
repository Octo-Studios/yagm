package it.hurts.sskirillss.yagm.data.entitydata;


import lombok.Data;


@Data
public class GhostEntityData {
    public static final int SHY_DURATION_TICKS = 60;
    public static final int FEEDS_TO_TAME = 3;
    public static final double CEMETERY_AGGRO_RADIUS = 16.0;

    public static final double HOVER_HEIGHT = 2.5;
    public static final double FLY_SPEED = 0.08;
    public static final double SMOOTH_FACTOR = 0.07;
    public static final double ARRIVAL_THRESHOLD = 1.2;
    public static final int STEAL_DELAY_TICKS = 40;
    public static final int MAX_STOLEN_ITEMS = 27;
}
