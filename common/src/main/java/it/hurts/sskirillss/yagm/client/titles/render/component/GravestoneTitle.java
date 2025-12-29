package it.hurts.sskirillss.yagm.client.titles.render.component;

import it.hurts.sskirillss.yagm.client.titles.render.type.GravestoneTitleType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;
import net.minecraft.network.chat.Component;


@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GravestoneTitle {

    private final GravestoneTitleType type;
    @With private final Component text;
    @With private final int color;
    @With private final boolean visible;

    public static GravestoneTitle of(GravestoneTitleType type, Component text) {
        return new GravestoneTitle(type, text, type.getDefaultColor(), true);
    }

    public static GravestoneTitle of(GravestoneTitleType type, Component text, int color) {
        return new GravestoneTitle(type, text, color, true);
    }

    public static GravestoneTitle of(GravestoneTitleType type, String text) {
        return of(type, Component.literal(text));
    }

    public static GravestoneTitle of(GravestoneTitleType type, String text, int color) {
        return of(type, Component.literal(text), color);
    }

    public static GravestoneTitle empty(GravestoneTitleType type) {
        return new GravestoneTitle(type, Component.empty(), type.getDefaultColor(), false);
    }

    public boolean isVisible() {
        return visible && text != null && !text.getString().isEmpty();
    }

    public float getScale() {
        return type.getScale();
    }

    public int getOrder() {
        return type.getOrder();
    }
}