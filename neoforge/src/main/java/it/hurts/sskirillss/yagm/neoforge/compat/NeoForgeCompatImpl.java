package it.hurts.sskirillss.yagm.neoforge.compat;

import it.hurts.sskirillss.yagm.api.compat.AccessoryLoader;
import it.hurts.sskirillss.yagm.api.compat.runnable.CompatRunnable;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.api.compat.twilight.TwilightForestCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.accessories.AccessoriesCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.backpack.NeoForgeBackPackedCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.backpack.NeoForgeSophisticatedBackpacksCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.backpack.NeoForgeTravelersBackpackCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.CosmeticArmorCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.CuriosCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import it.hurts.sskirillss.yagm.neoforge.compat.twilight.TwilightForestNeoForgeEvents;
import it.hurts.sskirillss.yagm.neoforge.event.NeoForgeDeathEvents;
import net.neoforged.bus.api.IEventBus;

public class NeoForgeCompatImpl {

    public static void registerPlatformHandlers() {
        TwilightForestCompat.setdeathHandler();
        TwilightForestNeoForgeEvents.register();

        NeoForgeDeathEvents.register();

        CompatRunnable.runFirst(
                CompatRunnable.getModId("accessories", () -> AccessoryLoader.registerHandler(new AccessoriesCompat())),
                CompatRunnable.getModId("curios", () -> AccessoryLoader.registerHandler(new CuriosCompat()))
        );

        CompatRunnable.run(
                CompatRunnable.getModId("cosmeticarmorreworked", () -> AccessoryLoader.registerHandler(new CosmeticArmorCompat())),
                CompatRunnable.getModId("backpacked", () -> BackpackLoader.registerHandler(new NeoForgeBackPackedCompat())),
                CompatRunnable.getModId("travelersbackpack", () -> BackpackLoader.registerHandler(new NeoForgeTravelersBackpackCompat())),
                CompatRunnable.getModId("sophisticatedbackpacks", () -> BackpackLoader.registerItemHandler(new NeoForgeSophisticatedBackpacksCompat()))
        );
    }

    public static void registerDataComponents(IEventBus modEventBus) {
        CompatRunnable.run(CompatRunnable.getModId("curios", () -> CurioSlotData.register(modEventBus)));
    }
}
