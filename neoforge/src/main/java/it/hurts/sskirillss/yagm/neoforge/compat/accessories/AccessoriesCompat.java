package it.hurts.sskirillss.yagm.neoforge.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.accessories.AccessoriesCompatImpl;
import net.neoforged.fml.ModList;

public class AccessoriesCompat extends AccessoriesCompatImpl {

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("accessories");
    }
}
