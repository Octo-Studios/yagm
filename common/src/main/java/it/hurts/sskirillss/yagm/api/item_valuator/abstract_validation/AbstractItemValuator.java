package it.hurts.sskirillss.yagm.api.item_valuator.abstract_validation;

import it.hurts.sskirillss.yagm.api.item_valuator.providers.ILevelDeterminer;
import it.hurts.sskirillss.yagm.api.item_valuator.config.ValuatorConfig;
import it.hurts.sskirillss.yagm.api.item_valuator.provider.ValueProviderRegistry;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Abstract item valuator.
 * Extendable to create custom logic.
 */
@Getter
public abstract class AbstractItemValuator {

    protected final MinecraftServer server;
    protected final ValuatorConfig config;
    protected final Map<Item, Double> valueCache = new ConcurrentHashMap<>();
    protected final List<Recipe<?>> allRecipes;

    protected AbstractItemValuator(MinecraftServer server, ValuatorConfig config) {
        this.server = server;
        this.config = config;
        this.allRecipes = loadRecipes();
    }

    public void initialize() {
        config.load();
        config.loadOverrides();

        computeBaseValues();
        computeRecipeValues();
        fillMissingValues();

        exportValues();

        onInitialized();
    }

    protected void onInitialized() {}


    protected List<Recipe<?>> loadRecipes() {
        return server.getRecipeManager()
                .getOrderedRecipes()
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("all")
    protected void computeBaseValues() {
        Set<Item> craftedItems = allRecipes.stream()
                .map(r -> r.getResultItem(server.registryAccess()))
                .filter(Objects::nonNull)
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::getItem)
                .collect(Collectors.toSet());

        Registry<Item> registry = getItemRegistry();

        for (Item item : registry) {
            ResourceLocation itemId = registry.getKey(item);
            Double override = config.getOverride(itemId);

            if (override != null) {
                valueCache.put(item, override);
            } else if (!craftedItems.contains(item)) {
                double base = computeBaseValue(item);
                valueCache.put(item, base);
            }
        }
    }

    protected double computeBaseValue(Item item) {
        ItemStack stack = new ItemStack(item);

        OptionalDouble providerValue = ValueProviderRegistry.getValue(stack);
        if (providerValue.isPresent()) {
            return providerValue.getAsDouble();
        }

        double value = config.getDefaultValue();
        value *= (1 + stack.getRarity().ordinal() * config.getRarityMultiplier());

        return ValueProviderRegistry.applyModifiers(stack, value);
    }

    protected void computeRecipeValues() {
        boolean changed;

        do {
            changed = false;

            for (Recipe<?> recipe : allRecipes) {
                ItemStack result = recipe.getResultItem(server.registryAccess());
                if (result.isEmpty()) continue;

                Item resultItem = result.getItem();
                if (valueCache.containsKey(resultItem)) continue;

                OptionalDouble recipeValue = computeRecipeValue(recipe);

                if (recipeValue.isPresent()) {
                    double perUnit = recipeValue.getAsDouble() / result.getCount();
                    perUnit = ValueProviderRegistry.applyModifiers(result, perUnit);
                    valueCache.put(resultItem, perUnit);
                    changed = true;
                }
            }
        } while (changed);
    }

    protected OptionalDouble computeRecipeValue(Recipe<?> recipe) {
        if (recipe instanceof SmithingRecipe smithing) {
            return computeSmithingValue(smithing);
        }

        double sum = 0;

        for (Ingredient ingredient : recipe.getIngredients()) {
            OptionalDouble ingredientValue = getMinIngredientValue(ingredient);
            if (ingredientValue.isEmpty()) {
                return OptionalDouble.empty();
            }
            sum += ingredientValue.getAsDouble();
        }

        return OptionalDouble.of(sum);
    }

    protected OptionalDouble computeSmithingValue(SmithingRecipe recipe) {
        Registry<Item> registry = getItemRegistry();

        OptionalDouble baseCost = registry.stream()
                .filter(item -> recipe.isBaseIngredient(new ItemStack(item)))
                .map(valueCache::get)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .min();

        OptionalDouble addCost = registry.stream()
                .filter(item -> recipe.isAdditionIngredient(new ItemStack(item)))
                .map(valueCache::get)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .min();

        if (baseCost.isEmpty() || addCost.isEmpty()) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(baseCost.getAsDouble() + addCost.getAsDouble());
    }

    protected OptionalDouble getMinIngredientValue(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems())
                .map(ItemStack::getItem)
                .map(valueCache::get)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .min();
    }

    @SuppressWarnings("all")
    protected void fillMissingValues() {
        Registry<Item> registry = getItemRegistry();

        for (Item item : registry) {
            valueCache.computeIfAbsent(item, i -> computeBaseValue(i));
        }
    }

    protected void exportValues() {
        Registry<Item> registry = getItemRegistry();
        Map<ResourceLocation, Double> values = new LinkedHashMap<>();

        for (Item item : registry) {
            ResourceLocation id = registry.getKey(item);
            Double value = valueCache.get(item);
            if (id != null && value != null) {
                values.put(id, value);
            }
        }

        config.exportItemValues(values);
    }


    public double getValue(Item item) {
        return valueCache.getOrDefault(item, config.getDefaultValue());
    }

    public double getStackValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        OptionalDouble providerValue = ValueProviderRegistry.getValue(stack);
        if (providerValue.isPresent()) {
            return providerValue.getAsDouble() * stack.getCount();
        }

        return getValue(stack.getItem()) * stack.getCount();
    }

    public double calculateInventoryValue(Player player) {
        Inventory inv = player.getInventory();
        return calculateValue(inv.items) + calculateValue(inv.armor) + calculateValue(inv.offhand);
    }

    @SafeVarargs
    public final double calculateValue(NonNullList<ItemStack>... lists) {
        double total = 0;
        for (NonNullList<ItemStack> list : lists) {
            if (list != null) {
                for (ItemStack stack : list) {
                    total += getStackValue(stack);
                }
            }
        }
        return total;
    }


    public <T> T determineLevel(double value, ILevelDeterminer<T> determiner) {
        T[] levels = determiner.getAllLevels();

        for (int i = levels.length - 1; i >= 0; i--) {
            if (value >= determiner.getThreshold(levels[i])) {
                return levels[i];
            }
        }

        return determiner.getDefault();
    }

    protected Registry<Item> getItemRegistry() {
        return server.registryAccess().registryOrThrow(Registries.ITEM);
    }


    public void clearCache() {
        valueCache.clear();
    }

    public void reload() {
        clearCache();
        initialize();
    }
}