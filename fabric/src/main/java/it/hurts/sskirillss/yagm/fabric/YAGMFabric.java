package it.hurts.sskirillss.yagm.fabric;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.api.compat.YAGMCompat;
import net.fabricmc.api.ModInitializer;

public class YAGMFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YAGMCommon.init();
        YAGMCompat.init();
    }
}