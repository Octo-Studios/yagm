package it.hurts.sskirillss.yagm.data;

import it.hurts.sskirillss.yagm.YAGMCommon;
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

public class GraveSaveManager {

    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public static boolean saveGraveData(String worldName, UUID playerUuid, String playerName, CompoundTag graveData) {
        try {
            Path basePath = Paths.get("saves", worldName, YAGMCommon.MODID.toLowerCase(Locale.ROOT), playerUuid.toString());
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
            return false;
        }
    }

    @Nullable
    public static CompoundTag loadGraveData(String worldName, UUID playerUuid, String saveName) {
        try {
            Path basePath = Paths.get("saves", worldName, YAGMCommon.MODID.toLowerCase(Locale.ROOT), playerUuid.toString());
            Path filePath = basePath.resolve(saveName);

            if (!Files.exists(filePath)) {
                YAGMCommon.LOGGER.warn("Grave save file not found: {}", filePath);
                return null;
            }

            CompoundTag saveTag;
            try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(filePath))) {
                saveTag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            }

            return saveTag.getCompound("GraveData");
        } catch (IOException e) {
            return null;
        }
    }

    public static List<String> listSaves(String worldName, UUID playerUuid) {
        List<String> saves = new ArrayList<>();
        Path basePath = Paths.get("saves", worldName, YAGMCommon.MODID.toLowerCase(Locale.ROOT), playerUuid.toString());

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
        try {
            Path basePath = Paths.get("saves", worldName, YAGMCommon.MODID.toLowerCase(Locale.ROOT), playerUuid.toString());
            Path filePath = basePath.resolve(saveName);

            if (!Files.exists(filePath)) {
                return false;
            }

            Files.delete(filePath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}