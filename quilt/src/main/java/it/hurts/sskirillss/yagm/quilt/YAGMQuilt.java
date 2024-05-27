package it.hurts.sskirillss.yagm.quilt;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;

public class YAGMQuilt implements ModInitializer {
    @Override
    public void onInitialize(ModContainer mod) {
        it.hurts.sskirillss.yagm.fabriclike.YAGMFabricLike.init();
    }
}