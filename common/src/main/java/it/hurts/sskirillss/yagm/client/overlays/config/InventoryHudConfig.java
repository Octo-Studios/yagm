package it.hurts.sskirillss.yagm.client.overlays.config;

import lombok.Getter;

@Getter
public final class InventoryHudConfig {
    private static final InventoryHudConfig INSTANCE = new InventoryHudConfig();

    private final int slot = 18;
    private final int pad = 8;
    private final int armorGap = 4;
    private final int hotbarGap = 2;
    private final int sectionGap = 6;
    private final int lineHeight = 10;
    private final int panelWidth = pad + slot + armorGap + 9 * slot + pad;
    private final int contentWidth = panelWidth - pad * 2;
    private final int accessoriesPerRow = contentWidth / slot;

    private final int backgroundColor = 0x00000000;
    private final int nameColor = 0xFFFFFFFF;
    private final int labelColor = 0xFFAAAAAA;
    private final int causeColor = 0xFFFF7070;

    public static InventoryHudConfig get() {
        return INSTANCE;
    }
}
