package it.hurts.sskirillss.yagm.neoforge.compat.twilight;

import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Map;
import java.util.Optional;

public final class TwilightCharmSlotHook {
    private static final String CHARM_STACK_TAG = "CharmStack";

    private enum CharmSource { NONE, ACCESSORIES, CURIOS }

    private static final ThreadLocal<CharmSource> pendingSource = ThreadLocal.withInitial(() -> CharmSource.NONE);
    private static final ThreadLocal<AccessorySlot> pendingSlot   = ThreadLocal.withInitial(() -> null);

    public static boolean consumeEquippedCharm(Item item, Player player) {
        AccessorySlot slot = findAccessoryCharmSlot(item, player);
        if (slot != null) {
            pendingSource.set(CharmSource.ACCESSORIES);
            pendingSlot.set(slot);
            return true;
        }

        if (Platform.isModLoaded("curios")) {
            Boolean consumed = callStatic("twilightforest.compat.curios.CuriosCompat", "findAndConsumeCurio", new Class[]{Item.class, Player.class}, item, player);
            if (Boolean.TRUE.equals(consumed)) {
                pendingSource.set(CharmSource.CURIOS);
                return true;
            }
        }

        pendingSource.set(CharmSource.NONE);
        return false;
    }

    public static boolean consumeInventoryItemPreferringEquipped(Player player, ItemLike itemLike, CompoundTag data, boolean saveCharm) {
        CharmSource source = pendingSource.get();
        AccessorySlot slot = pendingSlot.get();
        pendingSource.set(CharmSource.NONE);
        pendingSlot.set(null);

        switch (source) {
            case ACCESSORIES -> {
                if (slot != null) {
                    ItemStack stack = slot.container().getItem(slot.index()).copy();
                    slot.consume();
                    if (saveCharm) {
                        data.put(CHARM_STACK_TAG, stack.save(player.registryAccess()));
                    }
                    return true;
                }
            }
            case CURIOS -> {
                return false;
            }
            default -> {
                if (hasEquippedCharm(itemLike.asItem(), player)) return false;

                Boolean result = callStatic("twilightforest.util.TFItemStackUtils", "consumeInventoryItem", new Class[]{Player.class, ItemLike.class, CompoundTag.class, boolean.class}, player, itemLike, data, saveCharm);
                return Boolean.TRUE.equals(result);
            }
        }

        return false;
    }


    private static boolean hasEquippedCharm(Item item, Player player) {
        return hasCurioCharm(item, player) || hasAccessoryCharm(item, player);
    }

    private static boolean hasCurioCharm(Item item, Player player) {
        if (!Platform.isModLoaded("curios")) return false;

        Object helper = callStatic("top.theillusivec4.curios.api.CuriosApi", "getCuriosHelper", new Class[0]);
        if (helper == null) return false;

        Optional<?> result = callOn(helper, "findEquippedCurio", new Class[]{Item.class, LivingEntity.class}, item, player);
        return result != null && result.isPresent();
    }

    private static boolean hasAccessoryCharm(Item item, Player player) {
        return findAccessoryCharmSlot(item, player) != null;
    }

    private static AccessorySlot findAccessoryCharmSlot(Item item, Player player) {
        if (!Platform.isModLoaded("accessories")) return null;

        Optional<?> optCapability = callStatic("io.wispforest.accessories.api.AccessoriesCapability", "getOptionally", new Class[]{LivingEntity.class}, player);
        if (optCapability == null || optCapability.isEmpty()) return null;

        Map<?, ?> containers = callOn(optCapability.get(), "getContainers", new Class[0]);
        if (containers == null) return null;

        for (Object container : containers.values()) {
            Container main = callOn(container, "getAccessories", new Class[0]);
            AccessorySlot slot = findSlotWithItem(item, container, main);
            if (slot != null) return slot;

            Container cosmetic = callOn(container, "getCosmeticAccessories", new Class[0]);
            slot = findSlotWithItem(item, container, cosmetic);
            if (slot != null) return slot;
        }

        return null;
    }

    private static AccessorySlot findSlotWithItem(Item item, Object owner, Container container) {
        if (container == null) return null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                return new AccessorySlot(owner, container, i);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T callStatic(String className, String methodName, Class<?>[] types, Object... args) {
        try {
            return (T) Class.forName(className).getMethod(methodName, types).invoke(null, args);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T callOn(Object instance, String methodName, Class<?>[] types, Object... args) {
        try {
            return (T) instance.getClass().getMethod(methodName, types).invoke(instance, args);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return null;
        }
    }


    private record AccessorySlot(Object owner, Container container, int index) {
        void consume() {
            var stack = container.getItem(index);
            if (stack.isEmpty()) return;

            stack.shrink(1);
            container.setChanged();
            callOn(owner, "markChanged", new Class[0]);
        }
    }
}
