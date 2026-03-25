package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.YAGMCommon;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
public class GraveSaveManager {

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final long MAX_NBT_SIZE = 64L * 1024 * 1024; // 64 MB cap

    private static boolean isValidSaveName(String saveName) {
        if (saveName == null || saveName.isEmpty()) return false;
        if (saveName.contains("..") || saveName.contains("/") || saveName.contains("\\")) return false;
        if (!saveName.endsWith(".dat")) return false;
        Path asPath = Paths.get(saveName);
        return asPath.getNameCount() == 1 && asPath.toString().equals(saveName);
    }

    private static Path getBasePath(String worldName, UUID playerUuid) {
        return Paths.get("saves", worldName, YAGMCommon.MODID.toLowerCase(Locale.ROOT), playerUuid.toString());
    }

    public static boolean saveGraveData(String worldName, UUID playerUuid, String playerName, CompoundTag graveData) {
        try {
            Path basePath = getBasePath(worldName, playerUuid);
            Files.createDirectories(basePath);

            String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
            String filename = timestamp + ".dat";
            Path filePath = basePath.resolve(filename);

            CompoundTag saveTag = new CompoundTag();
            saveTag.putString("PlayerName", playerName);
            saveTag.put("GraveData", graveData);

            try (BufferedOutputStream stream = new BufferedOutputStream(Files.newOutputStream(filePath))) {
                NbtIo.writeCompressed(saveTag, stream);
            }
            return true;
        } catch (IOException e) {
            log.error("Failed to save grave data for player {}", playerUuid, e);
            return false;
        }
    }

    @Nullable
    public static CompoundTag loadGraveData(String worldName, UUID playerUuid, String saveName) {
        if (!isValidSaveName(saveName)) {
            log.warn("Rejected invalid save name: {}", saveName);
            return null;
        }

        try {
            Path basePath = getBasePath(worldName, playerUuid);
            Path filePath = basePath.resolve(saveName);

            if (!filePath.normalize().startsWith(basePath.normalize())) {
                log.warn("Path traversal attempt detected: {}", saveName);
                return null;
            }

            if (!Files.exists(filePath)) {
                log.warn("Grave save file not found: {}", filePath);
                return null;
            }

            CompoundTag saveTag;
            try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(filePath))) {
                saveTag = NbtIo.readCompressed(stream, NbtAccounter.create(MAX_NBT_SIZE));
            }

            return saveTag.getCompound("GraveData");
        } catch (IOException e) {
            log.error("Failed to load grave data: {}", saveName, e);
            return null;
        }
    }

    public static List<String> listSaves(String worldName, UUID playerUuid) {
        List<String> saves = new ArrayList<>();
        Path basePath = getBasePath(worldName, playerUuid);

        if (!Files.exists(basePath)) {
            return saves;
        }

        try (Stream<Path> stream = Files.list(basePath)) {
            stream.filter(path -> path.toString().endsWith(".dat"))
                  .map(Path::getFileName)
                  .map(Path::toString)
                  .forEach(saves::add);
        } catch (IOException ignored) {}

        return saves;
    }

    public static boolean deleteSave(String worldName, UUID playerUuid, String saveName) {
        if (!isValidSaveName(saveName)) {
            log.warn("Rejected invalid save name for deletion: {}", saveName);
            return false;
        }

        try {
            Path basePath = getBasePath(worldName, playerUuid);
            Path filePath = basePath.resolve(saveName);

            if (!filePath.normalize().startsWith(basePath.normalize())) {
                log.warn("Path traversal attempt detected: {}", saveName);
                return false;
            }

            if (!Files.exists(filePath)) {
                return false;
            }

            Files.delete(filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to delete grave save: {}", saveName, e);
            return false;
        }
    }
}
