package it.hurts.sskirillss.yagm.data;

import it.hurts.sskirillss.yagm.YAGMCommon;
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
import java.util.stream.IntStream;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

public class GraveDataManager extends SavedData {
    
    private static final String DATA_NAME = YAGMCommon.MODID + "_graves";
    
    private final Map<UUID, CompoundTag> playerGraves = new HashMap<>();

    private final Map<UUID, NonNullList<ItemStack>> transientMain = new HashMap<>();
    private final Map<UUID, NonNullList<ItemStack>> transientArmor = new HashMap<>();
    private final Map<UUID, NonNullList<ItemStack>> transientOffhand = new HashMap<>();

    public void putTransientGrave(UUID graveId, NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        if (graveId == null) return;
        if (main != null) transientMain.put(graveId, NonNullList.withSize(main.size(), ItemStack.EMPTY));
        if (armor != null) transientArmor.put(graveId, NonNullList.withSize(armor.size(), ItemStack.EMPTY));
        if (offhand != null) transientOffhand.put(graveId, NonNullList.withSize(offhand.size(), ItemStack.EMPTY));


        if (main != null) {
            NonNullList<ItemStack> dest = transientMain.get(graveId);
            IntStream.range(0, main.size()).forEach(i -> dest.set(i, main.get(i).copy()));
        }
        if (armor != null) {
            NonNullList<ItemStack> dest = transientArmor.get(graveId);
            IntStream.range(0, armor.size()).forEach(i -> dest.set(i, armor.get(i).copy()));
        }
        if (offhand != null) {
            NonNullList<ItemStack> dest = transientOffhand.get(graveId);
            IntStream.range(0, offhand.size()).forEach(i -> dest.set(i, offhand.get(i).copy()));
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

    public GraveDataManager() {
        super();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag gravesList = new ListTag();
        gravesList.addAll(playerGraves.values());
        tag.put("Graves", gravesList);
        return tag;
    }

    public static GraveDataManager load(CompoundTag tag, HolderLookup.Provider registries) {
        GraveDataManager data = new GraveDataManager();
        if (tag.contains("Graves", Tag.TAG_LIST)) {
            ListTag gravesList = tag.getList("Graves", Tag.TAG_COMPOUND);
            for (int i = 0; i < gravesList.size(); i++) {
                CompoundTag graveTag = gravesList.getCompound(i);
                if (graveTag.hasUUID("PlayerUuid")) {
                    UUID playerUuid = graveTag.getUUID("PlayerUuid");
                    data.playerGraves.put(playerUuid, graveTag);
                }
            }
        }
        return data;
    }

    public static GraveDataManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                GraveDataManager::new,
                GraveDataManager::load,
                null
            ),
            DATA_NAME
        );
    }
    
    public void addGrave(UUID playerUuid, CompoundTag graveData) {
        playerGraves.put(playerUuid, graveData);
        setDirty();
    }
    
    public void removeGrave(UUID playerUuid) {
        playerGraves.remove(playerUuid);
        setDirty();
    }
    
    public CompoundTag getGrave(UUID playerUuid) {
        return playerGraves.get(playerUuid);
    }
    
    public boolean hasGrave(UUID playerUuid) {
        return playerGraves.containsKey(playerUuid);
    }
    
    public Map<UUID, CompoundTag> getAllGraves() {
        return new HashMap<>(playerGraves);
    }
    
    public void clearAllGraves() {
        playerGraves.clear();
        setDirty();
    }
    
    public int getGraveCount() {
        return playerGraves.size();
    }
    

    public Map<UUID, CompoundTag> getPlayerGraves(UUID playerUuid) {
        Map<UUID, CompoundTag> result = new HashMap<>();
        for (Map.Entry<UUID, CompoundTag> entry : playerGraves.entrySet()) {
            if (entry.getValue().hasUUID("PlayerUuid") && 
                entry.getValue().getUUID("PlayerUuid").equals(playerUuid)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public void removePlayerGraves(UUID playerUuid) {
        playerGraves.entrySet().removeIf(entry -> {
            CompoundTag grave = entry.getValue();
            return grave.hasUUID("PlayerUuid") && grave.getUUID("PlayerUuid").equals(playerUuid);
        });
        setDirty();
    }


    public void cleanupOldGraves(long maxAge) {
        long currentTime = System.currentTimeMillis();
        playerGraves.entrySet().removeIf(entry -> {
            CompoundTag grave = entry.getValue();
            if (grave.contains("DeathTime")) {
                long deathTime = grave.getLong("DeathTime");
                return (currentTime - deathTime) > maxAge;
            }
            return false;
        });
        setDirty();
    }
}