package it.hurts.sskirillss.yagm.fabric;

import it.hurts.sskirillss.yagm.fabriclike.YAGMFabricLike;
import net.fabricmc.api.ModInitializer;

public class YAGMFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        YAGMFabricLike.init();
    }
}