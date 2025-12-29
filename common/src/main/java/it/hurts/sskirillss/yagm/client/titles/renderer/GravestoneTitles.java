package it.hurts.sskirillss.yagm.client.titles.renderer;

import it.hurts.sskirillss.yagm.client.titles.render.component.GravestoneTitle;
import it.hurts.sskirillss.yagm.client.titles.render.type.GravestoneTitleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class GravestoneTitles {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String TAG_WHO = "WhoDied";
    private static final String TAG_WHEN = "WhenDied";
    private static final String TAG_CAUSE = "DeathCause";
    private static final String TAG_TESTAMENT = "Testament";
    private static final String TAG_COLORS = "Colors";

    private final Map<GravestoneTitleType, GravestoneTitle> titles = new EnumMap<>(GravestoneTitleType.class);

    private GravestoneTitles() {
        for (GravestoneTitleType type : GravestoneTitleType.values()) {
            titles.put(type, GravestoneTitle.empty(type));
        }
    }

    public static GravestoneTitles create() {
        return new GravestoneTitles();
    }


    public static GravestoneTitles forDeathWithLevel(@Nullable String owner, @Nullable Long deathTime, @Nullable Component deathCause, @Nullable String testament) {
        GravestoneTitles t = new GravestoneTitles();

        if (owner != null && !owner.isEmpty()) {
            t.setTitle(GravestoneTitle.of(GravestoneTitleType.WHO_DIED, owner));
        }

        if (deathTime != null && deathTime > 0) {
            String formattedDate = Instant.ofEpochMilli(deathTime).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER);
            t.setTitle(GravestoneTitle.of(GravestoneTitleType.WHEN_DIED, formattedDate));
        }

        if (deathCause != null && !deathCause.getString().isEmpty()) {
            t.setTitle(GravestoneTitle.of(GravestoneTitleType.DEATH_CAUSE, deathCause));
        }

        if (testament != null && !testament.isEmpty()) {
            t.setTitle(GravestoneTitle.of(GravestoneTitleType.TESTAMENT, testament));
        }

        return t;
    }

    public GravestoneTitles setTitle(GravestoneTitle title) {
        titles.put(title.getType(), title);
        return this;
    }

    public GravestoneTitles setTitle(GravestoneTitleType type, String text) {
        return setTitle(GravestoneTitle.of(type, text));
    }

    public GravestoneTitles setTitle(GravestoneTitleType type, Component text) {
        return setTitle(GravestoneTitle.of(type, text));
    }


    public GravestoneTitle getTitle(GravestoneTitleType type) {
        return titles.getOrDefault(type, GravestoneTitle.empty(type));
    }

    public GravestoneTitle getWhoDied() { return getTitle(GravestoneTitleType.WHO_DIED); }
    public GravestoneTitle getWhenDied() { return getTitle(GravestoneTitleType.WHEN_DIED); }
    public GravestoneTitle getDeathCause() { return getTitle(GravestoneTitleType.DEATH_CAUSE); }
    public GravestoneTitle getTestament() { return getTitle(GravestoneTitleType.TESTAMENT); }


    public GravestoneTitles hideTitle(GravestoneTitleType type) {
        GravestoneTitle current = titles.get(type);
        if (current != null) titles.put(type, current.withVisible(false));
        return this;
    }

    public GravestoneTitles showTitle(GravestoneTitleType type) {
        GravestoneTitle current = titles.get(type);
        if (current != null) titles.put(type, current.withVisible(true));
        return this;
    }


    public GravestoneTitle[] getVisibleTitles() {
        return titles.values().stream().filter(GravestoneTitle::isVisible).sorted(Comparator.comparingInt(GravestoneTitle::getOrder)).toArray(GravestoneTitle[]::new);
    }

    public GravestoneTitle[] getAllTitles() {
        return titles.values().stream().sorted(Comparator.comparingInt(GravestoneTitle::getOrder)).toArray(GravestoneTitle[]::new);
    }


    public static GravestoneTitles load(CompoundTag tag) {
        GravestoneTitles t = new GravestoneTitles();

        if (tag.contains(TAG_WHO)) t.setTitle(GravestoneTitleType.WHO_DIED, tag.getString(TAG_WHO));
        if (tag.contains(TAG_WHEN)) t.setTitle(GravestoneTitleType.WHEN_DIED, tag.getString(TAG_WHEN));

        if (tag.contains(TAG_CAUSE)) {
            t.setTitle(GravestoneTitleType.DEATH_CAUSE, tag.getString(TAG_CAUSE));
        }

        if (tag.contains(TAG_TESTAMENT)) {
            String testament = tag.getString(TAG_TESTAMENT);
            if (!testament.isEmpty()) t.setTitle(GravestoneTitleType.TESTAMENT, testament);
        }

        if (tag.contains(TAG_COLORS)) {
            CompoundTag colors = tag.getCompound(TAG_COLORS);
            for (GravestoneTitleType type : GravestoneTitleType.values()) {
                if (colors.contains(type.name())) {
                    GravestoneTitle current = t.titles.get(type);
                    if (current != null) t.titles.put(type, current.withColor(colors.getInt(type.name())));
                }
            }
        }

        return t;
    }
}