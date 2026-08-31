package it.hurts.sskirillss.yagm.api.compat.config;

import dev.architectury.platform.Platform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public class CompatTomlConfig {

    public static boolean readBoolean(String fileName, String key, boolean defaultValue) {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(key, "key");

        Path path = Platform.getConfigFolder().resolve(fileName);
        if (!Files.isRegularFile(path)) {
            return defaultValue;
        }

        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String value = readBooleanValue(line, key);
                if (value == null) {
                    continue;
                }

                return switch (value.toLowerCase(Locale.ROOT)) {
                    case "true" -> true;
                    case "false" -> false;
                    default -> defaultValue;
                };
            }
        } catch (IOException ignored) {
            return defaultValue;
        }

        return defaultValue;
    }

    private static String readBooleanValue(String line, String key) {
        String stripped = stripComment(line).trim();
        int separator = stripped.indexOf('=');

        if (separator < 0) {
            return null;
        }

        String name = stripped.substring(0, separator).trim();
        if (!key.equals(name)) {
            return null;
        }

        return stripped.substring(separator + 1).trim();
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');

        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }
}
