package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GraveDataManager extends SavedData {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final String TAG_BLOCK_POS = "BlockPos";
    private static final String TAG_RESTORE_KEY_CONSUMED = "RestoreKeyConsumed";
    private final Map<UUID, CompoundTag> graves = new HashMap<>();
    private final Set<UUID> restoreKeyConsumed = new HashSet<>();

    public void addGrave(CompoundTag graveData) {
        if (!graveData.hasUUID(KEYS.getId())) return;

        UUID graveId = graveData.getUUID(KEYS.getId());
        graves.put(graveId, graveData.copy());
        restoreKeyConsumed.remove(graveId);


        setDirty();
    }

    public void removeGrave(UUID graveId) {
        if (graveId != null && graves.remove(graveId) != null) setDirty();
    }

    public boolean hasGrave(UUID graveId) {
        return graveId != null && graves.containsKey(graveId);
    }

    public CompoundTag getGrave(UUID graveId) {
        if (graveId == null) {
            return null;
        }

        CompoundTag data = graves.get(graveId);
        return data == null ? null : data.copy();
    }

    public void markRestoreKeyConsumed(UUID graveId) {
        if (graveId != null && restoreKeyConsumed.add(graveId)) {
            setDirty();
        }
    }

    public boolean isRestoreKeyConsumed(UUID graveId) {
        return graveId != null && restoreKeyConsumed.contains(graveId);
    }

    public void setGravePos(UUID graveId, BlockPos pos) {
        if (graveId == null || pos == null) {
            return;
        }

        CompoundTag graveData = graves.get(graveId);
        if (graveData == null) {
            return;
        }

        graveData.putLong(TAG_BLOCK_POS, pos.asLong());
        setDirty();
    }

    public BlockPos getGravePos(UUID graveId) {
        if (graveId == null) {
            return null;
        }

        CompoundTag graveData = graves.get(graveId);
        if (graveData == null || !graveData.contains(TAG_BLOCK_POS, Tag.TAG_LONG)) {
            return null;
        }

        return BlockPos.of(graveData.getLong(TAG_BLOCK_POS));
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        list.addAll(graves.values());

        tag.put(KEYS.getGraves(), list);

        ListTag consumed = new ListTag();
        for (UUID graveId : restoreKeyConsumed) {
            consumed.add(StringTag.valueOf(graveId.toString()));
        }
        tag.put(TAG_RESTORE_KEY_CONSUMED, consumed);
        return tag;
    }

    public static GraveDataManager load(CompoundTag tag, HolderLookup.Provider registries) {
        GraveDataManager data = new GraveDataManager();
        if (tag.contains(KEYS.getGraves(), Tag.TAG_LIST)) {
            ListTag list = tag.getList(KEYS.getGraves(), Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag graveTag = list.getCompound(i);
                if (graveTag.hasUUID(KEYS.getId())) {
                    data.graves.put(graveTag.getUUID(KEYS.getId()), graveTag);
                } else if (graveTag.hasUUID(KEYS.getPlayerId())) {
                    UUID id = UUID.randomUUID();
                    graveTag.putUUID(KEYS.getId(), id);
                    data.graves.put(id, graveTag);
                }
            }
        }

        if (tag.contains(TAG_RESTORE_KEY_CONSUMED, Tag.TAG_LIST)) {
            ListTag consumed = tag.getList(TAG_RESTORE_KEY_CONSUMED, Tag.TAG_STRING);
            for (int i = 0; i < consumed.size(); i++) {
                try {
                    data.restoreKeyConsumed.add(UUID.fromString(consumed.getString(i)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        return data;
    }

    public static GraveDataManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(GraveDataManager::new, GraveDataManager::load, null), "_graves");
    }
}
