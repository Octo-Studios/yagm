package it.hurts.sskirillss.yagm.data.gravedata;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.util.NbtKeys;
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
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
public class GraveSaveManager {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

    private static Path getGraveDir(String world, UUID uuid) {
        return Paths.get("saves", world, YAGMCommon.MODID.toLowerCase(Locale.ROOT), uuid.toString());
    }


    public static String formatSaveDisplayName(String filename) {
        if (filename.startsWith("death_") && filename.endsWith(".dat")) {
            String ts = filename.substring(6, filename.length() - 4);
            String[] parts = ts.split("_", 2);
            if (parts.length == 2) {
                return parts[0] + " " + parts[1].replace('-', ':');
            }
        }
        return filename;
    }

    @Nullable
    public static CompoundTag loadGraveData(String world, UUID uuid, String name) {
        Path dir = getGraveDir(world, uuid);
        Path file = dir.resolve(name);

        if (!file.normalize().startsWith(dir.normalize()) || !Files.exists(file)) return null;

        try (var in = new BufferedInputStream(Files.newInputStream(file))) {
            return NbtIo.readCompressed(in, NbtAccounter.create(64L * 1024 * 1024)).getCompound(KEYS.getGraveData());
        } catch (IOException e) {
            log.error("Load failed: {}", name, e);
            return null;
        }
    }

    public static void saveGraveData(String world, UUID uuid, long deathTimeMillis, CompoundTag graveData) {
        Path dir = getGraveDir(world, uuid);
        try {
            Files.createDirectories(dir);
            String timestamp = FILE_FORMATTER.format(Instant.ofEpochMilli(deathTimeMillis));
            Path file = dir.resolve("death_" + timestamp + ".dat");
            CompoundTag root = new CompoundTag();
            root.put(KEYS.getGraveData(), graveData);
            try (var out = new BufferedOutputStream(Files.newOutputStream(file))) {
                NbtIo.writeCompressed(root, out);
            }
        } catch (IOException e) {
            log.error("Save failed for {}", uuid, e);
        }
    }

    public static List<String> listSaves(String world, UUID uuid) {
        Path dir = getGraveDir(world, uuid);
        if (!Files.exists(dir)) return List.of();

        try (var s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".dat"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
