package it.hurts.sskirillss.yagm.fabric.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.accessories.BaseAccessoriesCompat;
import net.fabricmc.loader.api.FabricLoader;


public class AccessoriesCompat extends BaseAccessoriesCompat {

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("accessories");
    }
}
