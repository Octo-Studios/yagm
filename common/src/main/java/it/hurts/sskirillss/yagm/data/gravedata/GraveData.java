package it.hurts.sskirillss.yagm.data.gravedata;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Data
public class GraveData {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    public static final Codec<GraveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.optionalFieldOf(KEYS.getOwnerUuid()).forGetter(d -> Optional.ofNullable(d.ownerUUID)),
            Codec.STRING.optionalFieldOf(KEYS.getOwnerName()).forGetter(d -> Optional.ofNullable(d.ownerName)),
            UUIDUtil.CODEC.fieldOf(KEYS.getGraveId()).forGetter(GraveData::getGraveId),
            Codec.LONG.fieldOf(KEYS.getDeathTime()).forGetter(GraveData::getDeathTime),
            Codec.STRING.optionalFieldOf(KEYS.getTestament()).forGetter(d -> Optional.ofNullable(d.testament)),
            GraveStoneLevels.CODEC.fieldOf(KEYS.getGraveLevel()).forGetter(GraveData::getGraveLevel),
            ResourceLocation.CODEC.optionalFieldOf(KEYS.getVariantId()).forGetter(d -> Optional.ofNullable(d.variantId))
    ).apply(instance, GraveData::create));

    private UUID ownerUUID;
    private String ownerName;
    private UUID graveId;
    private long deathTime;
    private Component deathCause;
    private String testament;
    private GraveStoneLevels graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;
    private ResourceLocation variantId;

    public GraveData() {
        graveId = UUID.randomUUID();
        deathTime = System.currentTimeMillis();
    }

    private GraveData(UUID graveId, long deathTime) {
        this.graveId = graveId;
        this.deathTime = deathTime;
        this.graveLevel = GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }

    private static GraveData create(Optional<UUID> ownerUUID, Optional<String> ownerName, UUID graveId, long deathTime, Optional<String> testament, GraveStoneLevels graveLevel, Optional<ResourceLocation> variantId) {
        GraveData data = new GraveData(graveId, deathTime);
        ownerUUID.ifPresent(data::setOwnerUUID);
        ownerName.ifPresent(data::setOwnerName);
        testament.ifPresent(data::setTestament);
        data.setGraveLevel(graveLevel);
        variantId.ifPresent(data::setVariantId);
        return data;
    }

    public boolean isDecorative() {
        return ownerUUID == null;
    }

    public void init(UUID uuid, String name, long time, @Nullable Component cause, @Nullable String test, GraveStoneLevels level) {
        ownerUUID = uuid;
        ownerName = name;
        deathTime = time;
        deathCause = cause;
        testament = test;
        graveLevel = level;
    }

    public void save(CompoundTag tag, HolderLookup.Provider provider) {
        CODEC.encodeStart(NbtOps.INSTANCE, this).ifSuccess(encoded -> tag.merge((CompoundTag) encoded));

        if (deathCause != null) {
            tag.putString(KEYS.getDeathCause(), Component.Serializer.toJson(deathCause, provider));
        }
    }

    public static GraveData load(CompoundTag tag, HolderLookup.Provider provider) {
        GraveData data = CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial(e -> log.error("Failed to load GraveData: {}", e)).orElseGet(GraveData::new);

        if (tag.contains(KEYS.getDeathCause())) {
            try {
                data.deathCause = Component.Serializer.fromJson(tag.getString(KEYS.getDeathCause()), provider);
            } catch (Exception e) {
                data.deathCause = Component.literal(tag.getString(KEYS.getDeathCause()));
            }
        }

        return data;
    }
}
