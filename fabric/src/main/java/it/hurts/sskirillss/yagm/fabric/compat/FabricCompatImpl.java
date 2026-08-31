package it.hurts.sskirillss.yagm.fabric.compat;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.runnable.CompatRunnable;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.event.death.tracker.GraveDeathTracker;
import it.hurts.sskirillss.yagm.fabric.compat.accessories.AccessoriesCompat;
import it.hurts.sskirillss.yagm.fabric.compat.backpack.FabricBackPackedCompat;
import it.hurts.sskirillss.yagm.fabric.compat.backpack.FabricTravelersBackpackCompat;
import it.hurts.sskirillss.yagm.fabric.compat.trinkets.TrinketsCompat;

public class FabricCompatImpl {

    public static void registerPlatformHandlers() {
        GraveDeathTracker.getdeathhandler();

        CompatRunnable.runFirst(
                CompatRunnable.getModId("accessories", () -> AccessoryLoader.registerHandler(new AccessoriesCompat())),
                CompatRunnable.getModId("trinkets", () -> AccessoryLoader.registerHandler(new TrinketsCompat()))
        );

        CompatRunnable.run(
                CompatRunnable.getModId("backpacked", () -> BackpackLoader.registerHandler(new FabricBackPackedCompat())),
                CompatRunnable.getModId("travelersbackpack", () -> BackpackLoader.registerHandler(new FabricTravelersBackpackCompat()))
        );
    }
}
