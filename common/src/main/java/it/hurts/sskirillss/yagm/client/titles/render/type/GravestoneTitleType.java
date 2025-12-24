package it.hurts.sskirillss.yagm.client.titles.render.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.network.chat.Component;

@Getter
@RequiredArgsConstructor
public enum GravestoneTitleType {
    WHO_DIED(0, "title.yagm.who_died", 0xFFFFFF, 1.0f),
    WHEN_DIED(1, "title.yagm.when_died", 0xAAAAAA, 0.75f),
    DEATH_CAUSE(2, "title.yagm.death_cause", 0xFF6666, 0.75f),
    TESTAMENT(3, "title.yagm.testament", 0xFFD700, 0.6f);

    private final int order;
    private final String translationKey;
    private final int defaultColor;
    private final float scale;

    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    public static GravestoneTitleType byOrder(int order) {
        for (GravestoneTitleType type : values()) {
            if (type.order == order) {
                return type;
            }
        }
        return WHO_DIED;
    }
}