package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.util.NbtKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GraveSaveManager {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final String TAG_CONSUMED_LATEST = "ConsumedLatest";
    private static final String TAG_CONSUMED_MARKER = "Marker";
    private static final String TAG_CONSUMED_SORT_TIME = "SortTime";
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy:MM:dd-HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Comparator<CompoundTag> SAVE_ASCENDING = Comparator.comparingLong(GraveSaveManager::extractSortTime);


    public static String formatTime(CompoundTag saveData) {
        long time = saveData.getLong(KEYS.getSaveTime());

        if (time <= 0) {
            time = saveData.getLong(KEYS.getDeathTime());
        }

        return DISPLAY_FORMATTER.format(Instant.ofEpochMilli(time));
    }

    public static void saveGraveData(ServerLevel level, UUID uuid, long deathTimeMillis, CompoundTag graveData) {
        if (uuid == null || graveData == null || graveData.isEmpty()) {
            return;
        }

        CompoundTag copy = graveData.copy();

        if (!copy.hasUUID(KEYS.getId())) {
            copy.putUUID(KEYS.getId(), UUID.randomUUID());
        }

        copy.putLong(KEYS.getSaveTime(), deathTimeMillis);
        copy.putLong(KEYS.getDeathTime(), deathTimeMillis);

        GraveSavesData data = get(level);

        data.addSave(uuid, copy);

        data.setDirty();
    }

    @Nullable
    public static CompoundTag loadGraveData(ServerLevel level, UUID uuid, String selector) {
        List<CompoundTag> saves = listSaves(level, uuid);
        if (saves.isEmpty()) {
            return null;
        }

        for (CompoundTag save : saves) {
            if (matchesFormattedTime(save, selector)) {
                return save.copy();
            }
        }

        try {
            int index = Integer.parseInt(selector);
            if (index >= 1 && index <= saves.size()) {
                return saves.get(index - 1).copy();
            }
            return null;
        } catch (NumberFormatException ignored) {}

        UUID saveId;

        try {
            saveId = UUID.fromString(selector);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        for (CompoundTag save : saves) {
            if (save.hasUUID(KEYS.getId()) && save.getUUID(KEYS.getId()).equals(saveId)) {
                return save.copy();
            }
        }

        return null;
    }

    public static boolean matchesFormattedTime(CompoundTag saveData, String selector) {
        return formatTime(saveData).equals(selector);
    }

    public static List<CompoundTag> listSaves(ServerLevel level, UUID uuid) {
        GraveSavesData data = get(level);
        return data.getSaves(uuid).stream().map(CompoundTag::copy).toList();
    }

    @Nullable
    public static CompoundTag consumeLatestRestoreSave(ServerLevel level, UUID uuid) {
        GraveSavesData data = get(level);
        CompoundTag save = data.consumeLatestRestoreSave(uuid);
        if (save != null) {
            data.setDirty();
        }
        return save;
    }

    @Nullable
    public static CompoundTag peekLatestRestoreSave(ServerLevel level, UUID uuid) {
        return get(level).peekLatestRestoreSave(uuid);
    }

    private static GraveSavesData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();

        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(GraveSavesData::new, GraveSavesData::load, DataFixTypes.LEVEL), KEYS.getDataName());
    }

    private static long extractSortTime(CompoundTag saveData) {
        long saveTime = saveData.getLong(KEYS.getSaveTime());
        if (saveTime > 0) {
            return saveTime;
        }

        long deathTime = saveData.getLong(KEYS.getDeathTime());
        return Math.max(deathTime, 0L);
    }

    private static class GraveSavesData extends SavedData {
        private final Map<UUID, List<CompoundTag>> savesByPlayer = new HashMap<>();
        private final Map<UUID, String> consumedLatestMarkerByPlayer = new HashMap<>();
        private final Map<UUID, Long> consumedLatestSortTimeByPlayer = new HashMap<>();

        public void addSave(UUID playerId, CompoundTag saveData) {
            List<CompoundTag> saves = savesByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>());
            saves.add(saveData);
            saves.sort(SAVE_ASCENDING);
        }

        public List<CompoundTag> getSaves(UUID playerId) {
            List<CompoundTag> saves = savesByPlayer.get(playerId);
            if (saves == null || saves.isEmpty()) {
                return List.of();
            }

            return saves;
        }

        @Nullable
        public CompoundTag consumeLatestRestoreSave(UUID playerId) {
            List<CompoundTag> saves = savesByPlayer.get(playerId);
            if (saves == null || saves.isEmpty()) {
                return null;
            }

            CompoundTag latest = saves.getLast();
            String latestMarker = createMarker(latest);
            long latestSortTime = extractSortTime(latest);
            String consumedMarker = consumedLatestMarkerByPlayer.get(playerId);
            long consumedSortTime = consumedLatestSortTimeByPlayer.getOrDefault(playerId, -1L);

            if (latestMarker.equals(consumedMarker) || latestSortTime < consumedSortTime) {
                return null;
            }

            consumedLatestMarkerByPlayer.put(playerId, latestMarker);
            consumedLatestSortTimeByPlayer.put(playerId, latestSortTime);
            return latest.copy();
        }

        @Nullable
        public CompoundTag peekLatestRestoreSave(UUID playerId) {
            List<CompoundTag> saves = savesByPlayer.get(playerId);
            if (saves == null || saves.isEmpty()) {
                return null;
            }

            CompoundTag latest = saves.getLast();
            String latestMarker = createMarker(latest);
            long latestSortTime = extractSortTime(latest);
            String consumedMarker = consumedLatestMarkerByPlayer.get(playerId);
            long consumedSortTime = consumedLatestSortTimeByPlayer.getOrDefault(playerId, -1L);

            if (latestMarker.equals(consumedMarker) || latestSortTime < consumedSortTime) {
                return null;
            }

            return latest.copy();
        }

        public void removeLastSave(UUID playerId) {
            List<CompoundTag> saves = savesByPlayer.get(playerId);
            if (saves != null && !saves.isEmpty()) {
                saves.removeLast();
            }
        }

        @Override
        public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            ListTag players = new ListTag();

            for (Map.Entry<UUID, List<CompoundTag>> entry : savesByPlayer.entrySet()) {
                CompoundTag playerTag = new CompoundTag();
                playerTag.putUUID(KEYS.getPlayer(), entry.getKey());

                ListTag saves = new ListTag();
                for (CompoundTag saveTag : entry.getValue()) {
                    saves.add(saveTag.copy());
                }
                playerTag.put(KEYS.getSaves(), saves);
                players.add(playerTag);
            }

            tag.put(KEYS.getPlayers(), players);

            ListTag consumed = new ListTag();
            for (Map.Entry<UUID, String> entry : consumedLatestMarkerByPlayer.entrySet()) {
                CompoundTag consumedTag = new CompoundTag();
                consumedTag.putUUID(KEYS.getPlayer(), entry.getKey());
                consumedTag.putString(TAG_CONSUMED_MARKER, entry.getValue());
                consumedTag.putLong(TAG_CONSUMED_SORT_TIME, consumedLatestSortTimeByPlayer.getOrDefault(entry.getKey(), parseMarkerSortTime(entry.getValue())));
                consumed.add(consumedTag);
            }

            tag.put(TAG_CONSUMED_LATEST, consumed);
            return tag;
        }

        public static GraveSavesData load(CompoundTag tag, HolderLookup.Provider registries) {
            GraveSavesData data = new GraveSavesData();

            if (tag.contains(KEYS.getPlayers(), Tag.TAG_LIST)) {
                ListTag players = tag.getList(KEYS.getPlayers(), Tag.TAG_COMPOUND);
                for (int i = 0; i < players.size(); i++) {
                    CompoundTag playerTag = players.getCompound(i);
                    if (!playerTag.hasUUID(KEYS.getPlayer()) || !playerTag.contains(KEYS.getSaves(), Tag.TAG_LIST)) {
                        continue;
                    }

                    UUID playerId = playerTag.getUUID(KEYS.getPlayer());

                    ListTag saves = playerTag.getList(KEYS.getSaves(), Tag.TAG_COMPOUND);

                    List<CompoundTag> list = new ArrayList<>();

                    for (int j = 0; j < saves.size(); j++) {
                        list.add(saves.getCompound(j).copy());
                    }

                    list.sort(SAVE_ASCENDING);
                    data.savesByPlayer.put(playerId, list);
                }
            }

            if (tag.contains(TAG_CONSUMED_LATEST, Tag.TAG_LIST)) {
                ListTag consumed = tag.getList(TAG_CONSUMED_LATEST, Tag.TAG_COMPOUND);
                for (int i = 0; i < consumed.size(); i++) {
                    CompoundTag consumedTag = consumed.getCompound(i);
                    if (!consumedTag.hasUUID(KEYS.getPlayer()) || !consumedTag.contains(TAG_CONSUMED_MARKER, Tag.TAG_STRING)) {
                        continue;
                    }

                    UUID playerId = consumedTag.getUUID(KEYS.getPlayer());
                    String marker = consumedTag.getString(TAG_CONSUMED_MARKER);

                    data.consumedLatestMarkerByPlayer.put(playerId, marker);
                    data.consumedLatestSortTimeByPlayer.put(playerId, consumedTag.contains(TAG_CONSUMED_SORT_TIME, Tag.TAG_LONG) ? consumedTag.getLong(TAG_CONSUMED_SORT_TIME) : parseMarkerSortTime(marker));
                }
            }

            return data;
        }

        private static String createMarker(CompoundTag saveData) {
            long sortTime = extractSortTime(saveData);
            String idPart = saveData.hasUUID(KEYS.getId()) ? saveData.getUUID(KEYS.getId()).toString() : "no-id";
            return sortTime + "|" + idPart;
        }

        private static long parseMarkerSortTime(String marker) {
            if (marker == null || marker.isEmpty()) {
                return -1L;
            }

            int split = marker.indexOf('|');
            String time = split >= 0 ? marker.substring(0, split) : marker;

            try {
                return Long.parseLong(time);
            } catch (NumberFormatException ignored) {
                return -1L;
            }
        }
    }
}
