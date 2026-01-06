package it.hurts.sskirillss.yagm.api.item_valuator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ValuatorConfig {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeSpecialFloatingPointValues()
            .disableHtmlEscaping()
            .create();

    private final Path configDir;
    private final String modId;

    private Map<String, Double> itemOverrides = new HashMap<>();
    private double[] levelThresholds;
    private double defaultValue;
    private double rarityMultiplier;
    private boolean exportValues;

    public ValuatorConfig(Path configDir, String modId) {
        this.configDir = configDir;
        this.modId = modId;
    }

    public void load() {
        Path configFile = getConfigFile();

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                ConfigData data = GSON.fromJson(reader, ConfigData.class);
                if (data != null) {
                    applyData(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        Path configFile = getConfigFile();

        try {
            Files.createDirectories(configFile.getParent());

            try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(toData(), writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportItemValues(Map<ResourceLocation, Double> values) {
        if (!exportValues) return;

        Path exportFile = configDir.resolve(modId).resolve("item_values_export.json");

        try {
            Files.createDirectories(exportFile.getParent());

            Map<String, Double> sorted = new LinkedHashMap<>();
            values.entrySet().stream()
                    .sorted(Map.Entry.<ResourceLocation, Double>comparingByValue().reversed())
                    .forEach(e -> sorted.put(e.getKey().toString(), e.getValue()));

            try (Writer writer = Files.newBufferedWriter(exportFile, StandardCharsets.UTF_8)) {
                GSON.toJson(sorted, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadOverrides() {
        Path overridesFile = configDir.resolve(modId).resolve("item_overrides.json");

        if (Files.exists(overridesFile)) {
            try (Reader reader = Files.newBufferedReader(overridesFile, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, Double>>(){}.getType();
                Map<String, Double> overrides = GSON.fromJson(reader, type);
                if (overrides != null) {
                    this.itemOverrides.putAll(overrides);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Double getOverride(ResourceLocation itemId) {
        return itemOverrides.get(itemId.toString());
    }

    public double getThreshold(int levelIndex) {
        if (levelThresholds == null || levelIndex < 0 || levelIndex >= levelThresholds.length) {
            return 0;
        }
        return levelThresholds[levelIndex];
    }

    private Path getConfigFile() {
        return configDir.resolve(modId).resolve("valuator.json");
    }

    private void applyData(@NotNull ConfigData data) {
        if (data.levelThresholds != null) {
            this.levelThresholds = data.levelThresholds;
        }
        if (data.defaultValue != null) {
            this.defaultValue = data.defaultValue;
        }
        if (data.rarityMultiplier != null) {
            this.rarityMultiplier = data.rarityMultiplier;
        }
        if (data.exportValues != null) {
            this.exportValues = data.exportValues;
        }
    }

    private ConfigData toData() {
        ConfigData data = new ConfigData();
        data.levelThresholds = this.levelThresholds;
        data.defaultValue = this.defaultValue;
        data.rarityMultiplier = this.rarityMultiplier;
        data.exportValues = this.exportValues;
        return data;
    }

    private static class ConfigData {
        double[] levelThresholds;
        Double defaultValue;
        Double rarityMultiplier;
        Boolean exportValues;
    }
}