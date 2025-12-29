package it.hurts.sskirillss.yagm.utils;

import it.hurts.sskirillss.yagm.data_components.gravestones_types.GraveStoneLevels;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.io.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ItemValuator {

    private static ItemValuator INSTANCE;

    private final MinecraftServer server;
    private final Map<Item, Double> valueMap = new HashMap<>();
    private final List<Recipe<?>> allRecipes;
    private final List<BiFunction<ItemStack, Double, Double>> customAdjusters = new ArrayList<>();

    private static final double[] LEVEL_THRESHOLDS = {0, 100, 500, 2000, 10000};

    public static void initialize(MinecraftServer server) {
        server.submit(() -> {
            INSTANCE = new ItemValuator(server);
            INSTANCE.computeAllValues();
            INSTANCE.fillUnvaluedWithDefault();
            INSTANCE.exportToJson();
        });
    }

    public static void shutdown() {
        INSTANCE = null;
    }

    public static ItemValuator getInstance() {
        return INSTANCE;
    }

    public static boolean isAvailable() {
        return INSTANCE != null;
    }

    private ItemValuator(MinecraftServer server) {
        this.server = server;
        this.allRecipes = loadAllRecipes(server);

        customAdjusters.add((stack, value) -> stack.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "ingots"))) ? 10.0 : value);

        customAdjusters.add((stack, value) -> value * (1 + stack.getRarity().ordinal() * 0.1));

        initBaseValues();
    }

    private static List<Recipe<?>> loadAllRecipes(MinecraftServer server) {
        return server.getRecipeManager()
                .getOrderedRecipes()
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());
    }

    private void initBaseValues() {
        Set<Item> crafted = allRecipes.stream()
                .filter(r -> r.getResultItem(server.registryAccess()) != null)
                .map(r -> r.getResultItem(server.registryAccess()).getItem())
                .collect(Collectors.toSet());

        Registry<Item> reg = server.registryAccess().registryOrThrow(Registries.ITEM);
        for (Item item : reg) {
            if (!crafted.contains(item)) {
                double base = applyCustomAdjusters(new ItemStack(item), 1.0);
                valueMap.put(item, base);
            }
        }
    }

    public void computeAllValues() {
        boolean changed;
        Registry<Item> itemRegistry = server.registryAccess().registryOrThrow(Registries.ITEM);

        do {
            changed = false;
            for (Recipe<?> recipe : allRecipes) {
                ItemStack out = recipe.getResultItem(server.registryAccess());
                if (out == null || out.isEmpty()) continue;

                Item resultItem = out.getItem();
                if (valueMap.containsKey(resultItem)) continue;

                double sum = 0;
                boolean ok = true;

                if (recipe instanceof SmithingRecipe smith) {
                    OptionalDouble baseCost = itemRegistry.stream()
                            .filter(item -> smith.isBaseIngredient(new ItemStack(item)))
                            .map(valueMap::get)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .min();

                    OptionalDouble addCost = itemRegistry.stream()
                            .filter(item -> smith.isAdditionIngredient(new ItemStack(item)))
                            .map(valueMap::get)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .min();

                    if (baseCost.isEmpty() || addCost.isEmpty()) {
                        ok = false;
                    } else {
                        sum = baseCost.getAsDouble() + addCost.getAsDouble();
                    }
                } else {
                    for (Ingredient ing : recipe.getIngredients()) {
                        OptionalDouble any = Arrays.stream(ing.getItems())
                                .map(ItemStack::getItem)
                                .map(valueMap::get)
                                .filter(Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .min();
                        if (any.isEmpty()) {
                            ok = false;
                            break;
                        }
                        sum += any.getAsDouble();
                    }
                }

                if (!ok) continue;

                double perUnit = sum / out.getCount();
                perUnit = applyCustomAdjusters(out, perUnit);

                valueMap.put(resultItem, perUnit);
                changed = true;
            }
        } while (changed);
    }

    public void fillUnvaluedWithDefault() {
        Registry<Item> reg = server.registryAccess().registryOrThrow(Registries.ITEM);
        for (Item item : reg) {
            valueMap.putIfAbsent(item, applyCustomAdjusters(new ItemStack(item), 1.0));
        }
    }

    public void exportToJson() {
        List<Item> sortedItems = server.registryAccess()
                .registryOrThrow(Registries.ITEM)
                .stream()
                .sorted(Comparator.comparingDouble(this::getValue).reversed())
                .toList();

        Map<String, Double> outMap = new LinkedHashMap<>();
        for (Item item : sortedItems) {
            String id = server.registryAccess()
                    .registryOrThrow(Registries.ITEM)
                    .getKey(item)
                    .toString();
            outMap.put(id, getValue(item));
        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();

        File file = server.getServerDirectory().resolve("item_values.json").toFile();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(outMap, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double getValue(Item item) {
        return valueMap.getOrDefault(item, 0.0);
    }

    public double getStackValue(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;
        return getValue(stack.getItem()) * stack.getCount();
    }

    public double calculateInventoryValue(Player player) {
        Inventory inv = player.getInventory();
        double total = 0.0;

        for (ItemStack stack : inv.items) {
            total += getStackValue(stack);
        }
        for (ItemStack stack : inv.armor) {
            total += getStackValue(stack);
        }
        for (ItemStack stack : inv.offhand) {
            total += getStackValue(stack);
        }

        return total;
    }


    public double calculateListsValue(NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        double total = 0.0;

        if (main != null) {
            for (ItemStack stack : main) {
                total += getStackValue(stack);
            }
        }
        if (armor != null) {
            for (ItemStack stack : armor) {
                total += getStackValue(stack);
            }
        }
        if (offhand != null) {
            for (ItemStack stack : offhand) {
                total += getStackValue(stack);
            }
        }

        return total;
    }

    public GraveStoneLevels determineGraveLevel(Player player) {
        double value = calculateInventoryValue(player);
        return determineLevelByValue(value);
    }

    public GraveStoneLevels determineGraveLevel(NonNullList<ItemStack> main, NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand) {
        double value = calculateListsValue(main, armor, offhand);
        return determineLevelByValue(value);
    }

    public GraveStoneLevels determineLevelByValue(double value) {
        GraveStoneLevels[] levels = GraveStoneLevels.values();

        for (int i = levels.length - 1; i >= 0; i--) {
            if (value >= LEVEL_THRESHOLDS[i]) {
                return levels[i];
            }
        }

        return GraveStoneLevels.GRAVESTONE_LEVEL_1;
    }

    private double applyCustomAdjusters(ItemStack stack, double value) {
        for (BiFunction<ItemStack, Double, Double> adjuster : customAdjusters) {
            value = adjuster.apply(stack, value);
        }
        return value;
    }
}