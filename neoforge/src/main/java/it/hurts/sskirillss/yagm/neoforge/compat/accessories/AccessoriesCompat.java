package it.hurts.sskirillss.yagm.neoforge.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.accessories.BaseAccessoriesCompat;
import net.neoforged.fml.ModList;

/**
 * NeoForge-side registration for the Accessories mod.
 * All integration logic lives in {@link BaseAccessoriesCompat}.
 */
public class AccessoriesCompat extends BaseAccessoriesCompat {

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("accessories");
    }
}
