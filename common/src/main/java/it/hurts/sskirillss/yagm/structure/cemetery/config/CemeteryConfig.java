package it.hurts.sskirillss.yagm.structure.cemetery.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CemeteryConfig {

    public static int getDefaultRadius(){
        return 48;
    }

    public static int getDefaultMinGraves(){
        return 10;
    }

    public static int getCellSize(){
        return 16;
    }
}
