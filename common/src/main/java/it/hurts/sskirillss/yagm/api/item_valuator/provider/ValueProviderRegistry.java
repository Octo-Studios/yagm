package it.hurts.sskirillss.yagm.api.item_valuator.provider;

import it.hurts.sskirillss.yagm.api.item_valuator.providers.IItemValueProvider;
import it.hurts.sskirillss.yagm.api.item_valuator.providers.IValueModifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of item value providers.
 */
public final class ValueProviderRegistry {

    private static final Map<String, IItemValueProvider> PROVIDERS = new ConcurrentHashMap<>();
    private static final List<IItemValueProvider> SORTED_PROVIDERS = new ArrayList<>();
    private static final List<IValueModifier> MODIFIERS = new ArrayList<>();
    private static boolean needsSort = true;

    private ValueProviderRegistry() {}

    public static void registerProvider(IItemValueProvider provider) {
        PROVIDERS.put(provider.getId(), provider);
        needsSort = true;
    }

    /**
     * Gets the value of an item from all providers
     */
    public static void registerModifier(IValueModifier modifier) {
        MODIFIERS.add(modifier);
        MODIFIERS.sort(Comparator.comparingInt(IValueModifier::getPriority).reversed());
    }

    /**
     * Gets the value of an item from all providers
     */
    public static OptionalDouble getValue(ItemStack stack) {
        if (stack.isEmpty()) {
            return OptionalDouble.of(0);
        }

        ensureSorted();

        for (IItemValueProvider provider : SORTED_PROVIDERS) {
            OptionalDouble value = provider.getValue(stack);
            if (value.isPresent()) {
                return OptionalDouble.of(applyModifiers(stack, value.getAsDouble()));
            }
        }

        return OptionalDouble.empty();
    }

    /**
     * Gets the value of the item or the default
     */
    public static double getValueOrDefault(ItemStack stack, double defaultValue) {
        return getValue(stack).orElse(defaultValue);
    }

    /**
     * apply your modifier for value
     */
    public static double applyModifiers(ItemStack stack, double value) {
        for (IValueModifier modifier : MODIFIERS) {
            value = modifier.modify(stack, value);
        }
        return value;
    }

    public static IItemValueProvider getProvider(String id) {
        return PROVIDERS.get(id);
    }

    public static Collection<IItemValueProvider> getAllProviders() {
        return Collections.unmodifiableCollection(PROVIDERS.values());
    }

    public static List<IValueModifier> getAllModifiers() {
        return Collections.unmodifiableList(MODIFIERS);
    }

    private static void ensureSorted() {
        if (needsSort) {
            synchronized (SORTED_PROVIDERS) {
                SORTED_PROVIDERS.clear();
                SORTED_PROVIDERS.addAll(PROVIDERS.values());
                SORTED_PROVIDERS.sort(Comparator.comparingInt(IItemValueProvider::getPriority).reversed());
                needsSort = false;
            }
        }
    }

    public static void clear() {
        PROVIDERS.clear();
        SORTED_PROVIDERS.clear();
        MODIFIERS.clear();
        needsSort = true;
    }
}