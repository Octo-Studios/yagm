package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlacklistUtils {
    private static final TagKey<Enchantment> SOULBOUND = TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, "soulbound"));
    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    public static boolean isBlacklisted(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        if (stack.getEnchantments().keySet().stream().anyMatch(enchantment -> enchantment.is(SOULBOUND))) {
            return true;
        }

        var id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

        for (var entry : ConfigRegistry.CONFIG.getItemBlacklist()) {
            var value = entry.trim();

            if (value.isEmpty()) {
                continue;
            }

            if (value.startsWith("#")) {
                var tagId = ResourceLocation.tryParse(value.substring(1));

                if (tagId != null && stack.is(TagKey.create(Registries.ITEM, tagId))) {
                    return true;
                }
            } else if (matches(value, id)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matches(String regex, String id) {
        try {
            return PATTERNS.computeIfAbsent(regex, Pattern::compile).matcher(id).matches();
        } catch (PatternSyntaxException exception) {
            return regex.equals(id);
        }
    }
}
