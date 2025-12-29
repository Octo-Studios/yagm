package it.hurts.sskirillss.yagm.data;

import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Getter
@Setter
public class GraveData {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private UUID ownerUUID;
    private String ownerName;
    private UUID graveId;
    private long deathTime;
    private Component deathCause;
    private String testament;
    private GraveStoneLevels graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;

    public GraveData() {
        this.graveId = UUID.randomUUID();
        this.deathTime = System.currentTimeMillis();
    }

    public void initialize(UUID playerUUID, String playerName, long deathTime, @Nullable Component deathCause, @Nullable String testament, GraveStoneLevels level) {
        this.ownerUUID = playerUUID;
        this.ownerName = playerName;
        this.deathTime = deathTime;
        this.deathCause = deathCause;
        this.testament = testament;
        this.graveLevel = level;
    }

    public void loadFromTag(CompoundTag tag) {
        if (tag.hasUUID("OwnerUUID")) {
            this.ownerUUID = tag.getUUID("OwnerUUID");
        }
        this.ownerName = tag.getString("OwnerName");

        if (tag.contains("GraveId")) {
            this.graveId = tag.getUUID("GraveId");
        }
        this.deathTime = tag.getLong("DeathTime");

        if (tag.contains("DeathCause")) {
            this.deathCause = Component.literal(tag.getString("DeathCause"));
        }

        if (tag.contains("Testament")) {
            this.testament = tag.getString("Testament");
        }

        if (tag.contains("GraveLevel")) {
            this.graveLevel = GraveStoneLevels.CODEC.byName(
                    tag.getString("GraveLevel"),
                    GraveStoneLevels.GRAVESTONE_LEVEL_1
            );
        }
    }

    public void saveToTag(CompoundTag tag) {
        if (ownerUUID != null) {
            tag.putUUID("OwnerUUID", ownerUUID);
        }
        if (ownerName != null) {
            tag.putString("OwnerName", ownerName);
        }
        if (graveId != null) {
            tag.putUUID("GraveId", graveId);
        }
        tag.putLong("DeathTime", deathTime);

        if (deathCause != null) {
            tag.putString("DeathCause", deathCause.getString());
        }

        if (testament != null && !testament.isEmpty()) {
            tag.putString("Testament", testament);
        }

        tag.putString("GraveLevel", graveLevel.getSerializedName());
    }

    public void loadFromGraveData(CompoundTag data) {
        if (data.hasUUID("PlayerUuid")) {
            this.ownerUUID = data.getUUID("PlayerUuid");
        }
        this.ownerName = data.getString("PlayerName");

        if (data.hasUUID("Id")) {
            this.graveId = data.getUUID("Id");
        }

        if (data.contains("DeathTime")) {
            this.deathTime = data.getLong("DeathTime");
        }

        if (data.contains("DeathCause")) {
            this.deathCause = Component.literal(data.getString("DeathCause"));
        }

        if (data.contains("Testament")) {
            this.testament = data.getString("Testament");
        }
    }

    public String getOwnerNameOrDefault() {
        return ownerName != null ? ownerName : "Unknown";
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    public String getFormattedDeathTime() {
        Instant instant = Instant.ofEpochMilli(deathTime);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        return dateTime.format(DATE_FORMATTER);
    }

    public float getTextHeight() {
        return switch (graveLevel) {
            case GRAVESTONE_LEVEL_1 -> 1.0F;
            case GRAVESTONE_LEVEL_2 -> 1.2F;
            case GRAVESTONE_LEVEL_3 -> 1.4F;
            case GRAVESTONE_LEVEL_4 -> 1.6F;
            case GRAVESTONE_LEVEL_5 -> 1.8F;
        };
    }

    public int getTextColor() {
        return switch (graveLevel) {
            case GRAVESTONE_LEVEL_1 -> 0xFFAAAAAA;
            case GRAVESTONE_LEVEL_2 -> 0xFFFFFFFF;
            case GRAVESTONE_LEVEL_3 -> 0xFF55FF55;
            case GRAVESTONE_LEVEL_4 -> 0xFF5555FF;
            case GRAVESTONE_LEVEL_5 -> 0xFFFFAA00;
        };
    }
}