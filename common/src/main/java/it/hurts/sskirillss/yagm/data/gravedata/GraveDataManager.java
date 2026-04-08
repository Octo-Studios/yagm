package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraveDataManager extends SavedData {

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final String DATA_NAME = YAGMCommon.MODID + "_graves";
    private final Map<UUID, CompoundTag> graves = new HashMap<>();

    public void addGrave(CompoundTag graveData) {
        if (!graveData.hasUUID(KEYS.getId())) return;
        graves.put(graveData.getUUID(KEYS.getId()), graveData);


        setDirty();
    }

    public void removeGrave(UUID graveId) {
        if (graveId != null && graves.remove(graveId) != null) setDirty();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag list = new ListTag();
        list.addAll(graves.values());
        tag.put(KEYS.getGraves(), list);
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
        return data;
    }

    public static GraveDataManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(GraveDataManager::new, GraveDataManager::load, null), DATA_NAME);
    }
}
