package it.hurts.sskirillss.yagm.data.entitydata;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GhostEntityData {
    public static final int FEEDS_TO_TAME = 3;
    public static final int SHY_DURATION_TICKS = 60;
    public static final int MOOD_UPDATE_INTERVAL = 10;

    public static final double FLY_SPEED = 0.24;
    public static final float YAW_NORMAL = 0.12F;
    public static final float YAW_FOLLOW = 0.15F;
    public static final float PITCH_RETURN_SPEED = 0.08f;
    public static final float PITCH_MAX_DEGREES = 35f;

    public static final double TELEPORT_DISTANCE = 40.0;
    public static final double FOLLOW_ARRIVAL_DIST = 3.0;
    public static final double FOLLOW_ORBIT_RADIUS = 1.8;
    public static final double FOLLOW_ORBIT_HEIGHT = 1.5;
    public static final double FOLLOW_SPEED_MULT = 1.2;

    public static final double WANDER_SPEED_MULT = 0.5;
    public static final double WANDER_ARRIVAL_DIST = 2.0;
    public static final int WANDER_RETARGET_TICKS = 200;

    public static final double CEMETERY_AGGRO_RADIUS = 16.0;
    public static final double ATTACK_RANGE = 1.8;
    public static final double ATTACK_TICKS_INTERVAL = 20;
    public static final double CHARGE_SPEED = 0.85;
    public static final double CHARGE_ACCEL = 0.14;

    public static final int SPAWN_COOLDOWN_TICKS = 1200;
    public static final int MAX_GHOSTS_PER_CEMETERY = 5;
    public static final int CHECK_INTERVAL = 100;
    public static final double GHOST_COUNT_SEARCH_RADIUS = 64.0;

    public static final double WOBBLE_AMPLITUDE = 0.012;
    public static final double WOBBLE_SPEED = 0.08;
    public static final double BOB_AMPLITUDE = 0.006;
    public static final double BOB_SPEED = 0.12;

}
