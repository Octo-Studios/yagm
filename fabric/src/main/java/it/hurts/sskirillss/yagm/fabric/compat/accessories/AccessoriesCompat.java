package it.hurts.sskirillss.yagm.fabric.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.accessories.AccessoriesCompatImpl;
import net.fabricmc.loader.api.FabricLoader;

public class AccessoriesCompat extends AccessoriesCompatImpl {

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("accessories");
    }
}
