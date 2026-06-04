package it.hurts.sskirillss.yagm.api.compat.twilight;

import dev.architectury.platform.Platform;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class TwilightForestCompat {
    private static final String PLAYER_PERSISTED_TAG = "PlayerPersisted";
    private static final String CONSUMED_CHARM_TAG = "CharmStack";
    private static final ResourceLocation KEEPING_CHARM_3 = ResourceLocation.fromNamespaceAndPath("twilightforest", "charm_of_keeping_3");
    @Getter
    private static boolean lateDeathHandlerEnabled;

    public static void init() {
    }

    public static void enableLateDeathHandler() {
        lateDeathHandlerEnabled = Platform.isModLoaded("twilightforest");
    }

    public static boolean shouldSuppressGraveAfterTwilight(ServerPlayer player) {
        return KEEPING_CHARM_3.equals(getConsumedCharmId(player));
    }

    private static ResourceLocation getConsumedCharmId(ServerPlayer player) {
        try {
            Object result = player.getClass().getMethod("getPersistentData").invoke(player);
            if (!(result instanceof CompoundTag persistentData) || !persistentData.contains(PLAYER_PERSISTED_TAG, Tag.TAG_COMPOUND)) {
                return null;
            }

            CompoundTag persisted = persistentData.getCompound(PLAYER_PERSISTED_TAG);
            if (!persisted.contains(CONSUMED_CHARM_TAG, Tag.TAG_COMPOUND)) {
                return null;
            }

            ItemStack consumedCharm = ItemStack.parseOptional(player.registryAccess(), persisted.getCompound(CONSUMED_CHARM_TAG));
            return consumedCharm.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(consumedCharm.getItem());
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
