package it.hurts.sskirillss.yagm.neoforge;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.neoforge.compat.NeoForgeCompatImpl;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(YAGMCommon.MODID)
public class YAGMNeoForge {
    public YAGMNeoForge(IEventBus modBus) {
        NeoForgeCompatImpl.registerDataComponents(modBus);
        YAGMCommon.init();
    }
}