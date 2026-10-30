package it.hurts.sskirillss.yagm.init;

import it.hurts.octostudios.octolib.module.config.ConfigManager;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.config.YAGMConfig;

import java.util.Locale;

public class ConfigRegistry {
    public static final YAGMConfig CONFIG = new YAGMConfig();

    public static void init() {
        ConfigManager.registerConfig(YAGMCommon.MODID, CONFIG);
    }

    public static boolean isCompatEnabled(String modId) {
        return CONFIG.getEnabledCompats().stream().anyMatch(entry -> entry.trim().toLowerCase(Locale.ROOT).equals(modId));
    }
}
