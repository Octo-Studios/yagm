package it.hurts.sskirillss.yagm.structures.cemetery.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CemeteryConfig {

    private static final int DEFAULT_CLUSTER_RADIUS = 48;
    private static final int DEFAULT_MIN_GRAVES = 10;
    private static final int CELL_SIZE = 16;

    public static int getDefaultRadius(){
        return DEFAULT_CLUSTER_RADIUS;
    }

    public static int getDefaultMinGraves(){
        return DEFAULT_MIN_GRAVES;
    }

    public static int getCellSize(){
        return CELL_SIZE;
    }
}
