package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

public class GraveDataManager extends SavedData {

    private static final String DATA_NAME = YAGMCommon.MODID + "_graves";
    private final Map<UUID, CompoundTag> graves = new HashMap<>();

    private final Map<UUID, NonNullList<ItemStack>> transientMain = new HashMap<>();
    private final Map<UUID, NonNullList<ItemStack>> transientArmor = new HashMap<>();
    private final Map<UUID, NonNullList<ItemStack>> transientOffhand = new HashMap<>();

    public GraveDataManager() {
        super();
    }

    public void addGrave(UUID graveId, CompoundTag graveData) {
        if (graveId == null) return;
        graves.put(graveId, graveData);
        setDirty();
    }

    public void removeGrave(UUID graveId) {
        if (graveId == null) return;
        if (graves.remove(graveId) != null) {
            setDirty();
        }
    }

    @Nullable
    public CompoundTag getGrave(UUID graveId) {
        return graves.get(graveId);
    }

    public boolean hasGrave(UUID graveId) {
        return graves.containsKey(graveId);
    }

    public Map<UUID, CompoundTag> getAllGraves() {
        return new HashMap<>(graves);
    }


    public List<CompoundTag> getGravesForPlayer(UUID playerUuid) {
        List<CompoundTag> result = new ArrayList<>();
        for (CompoundTag tag : graves.values()) {
            if (tag.hasUUID("PlayerUuid") && tag.getUUID("PlayerUuid").equals(playerUuid)) {
                result.add(tag);
            }
        }
        return result;
    }


    public void putTransientGrave(UUID graveId, NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        if (graveId == null) return;
        if (main != null) {
            NonNullList<ItemStack> dest = NonNullList.withSize(main.size(), ItemStack.EMPTY);
            IntStream.range(0, main.size()).forEach(i -> dest.set(i, main.get(i).copy()));
            transientMain.put(graveId, dest);
        }
        if (armor != null) {
            NonNullList<ItemStack> dest = NonNullList.withSize(armor.size(), ItemStack.EMPTY);
            IntStream.range(0, armor.size()).forEach(i -> dest.set(i, armor.get(i).copy()));
            transientArmor.put(graveId, dest);
        }
        if (offhand != null) {
            NonNullList<ItemStack> dest = NonNullList.withSize(offhand.size(), ItemStack.EMPTY);
            IntStream.range(0, offhand.size()).forEach(i -> dest.set(i, offhand.get(i).copy()));
            transientOffhand.put(graveId, dest);
        }
    }

    public NonNullList<ItemStack> getTransientMain(UUID graveId) {
        return transientMain.get(graveId);
    }

    public NonNullList<ItemStack> getTransientArmor(UUID graveId) {
        return transientArmor.get(graveId);
    }

    public NonNullList<ItemStack> getTransientOffhand(UUID graveId) {
        return transientOffhand.get(graveId);
    }

    public void removeTransientGrave(UUID graveId) {
        transientMain.remove(graveId);
        transientArmor.remove(graveId);
        transientOffhand.remove(graveId);
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        ListTag gravesList = new ListTag();
        gravesList.addAll(graves.values());
        tag.put("Graves", gravesList);
        return tag;
    }

    public static GraveDataManager load(CompoundTag tag, HolderLookup.Provider registries) {
        GraveDataManager data = new GraveDataManager();
        if (tag.contains("Graves", Tag.TAG_LIST)) {
            ListTag gravesList = tag.getList("Graves", Tag.TAG_COMPOUND);
            for (int i = 0; i < gravesList.size(); i++) {
                CompoundTag graveTag = gravesList.getCompound(i);

                if (graveTag.hasUUID("Id")) {
                    data.graves.put(graveTag.getUUID("Id"), graveTag);
                } else if (graveTag.hasUUID("PlayerUuid")) {
                    UUID fallbackId = UUID.randomUUID();
                    graveTag.putUUID("Id", fallbackId);
                    data.graves.put(fallbackId, graveTag);
                }
            }
        }
        return data;
    }

    public static GraveDataManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(GraveDataManager::new, GraveDataManager::load, null), DATA_NAME);
    }
}
