package it.hurts.sskirillss.yagm.structure.cemetery.config;

import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CemeteryConfig {

    public static int getDefaultRadius(){
        return ConfigRegistry.CONFIG.getCemeteryRadius();
    }

    public static int getDefaultMinGraves(){
        return ConfigRegistry.CONFIG.getCemeteryMinGraves();
    }

    public static int getCellSize(){
        return 16;
    }
}
