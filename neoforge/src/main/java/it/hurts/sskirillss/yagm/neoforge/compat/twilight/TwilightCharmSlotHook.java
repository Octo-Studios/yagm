package it.hurts.sskirillss.yagm.neoforge.compat.twilight;

import dev.architectury.platform.Platform;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

public final class TwilightCharmSlotHook {
    private static final String CHARM_STACK_TAG = "CharmStack";

    private TwilightCharmSlotHook() {
    }

    public static boolean consumeInventoryItemPreferringEquipped(Player player, ItemLike itemLike, CompoundTag data, boolean saveCharm) {
        if (hasEquippedCharm(itemLike.asItem(), player)) {
            return false;
        }

        try {
            Class<?> utils = Class.forName("twilightforest.util.TFItemStackUtils");
            Object result = utils.getMethod("consumeInventoryItem", Player.class, ItemLike.class, CompoundTag.class, boolean.class)
                    .invoke(null, player, itemLike, data, saveCharm);
            return result instanceof Boolean consumed && consumed;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean consumeEquippedCharm(Item item, Player player) {
        if (consumeAccessoryCharm(item, player)) {
            return true;
        }

        if (!Platform.isModLoaded("curios")) {
            return false;
        }

        try {
            Class<?> curiosCompat = Class.forName("twilightforest.compat.curios.CuriosCompat");
            Object result = curiosCompat.getMethod("findAndConsumeCurio", Item.class, Player.class).invoke(null, item, player);
            return result instanceof Boolean consumed && consumed;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean hasEquippedCharm(Item item, Player player) {
        return hasCurioCharm(item, player) || hasAccessoryCharm(item, player);
    }

    private static boolean hasCurioCharm(Item item, Player player) {
        if (!Platform.isModLoaded("curios")) {
            return false;
        }

        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            Object helper = curiosApi.getMethod("getCuriosHelper").invoke(null);
            if (helper == null) {
                return false;
            }

            Object result = helper.getClass()
                    .getMethod("findEquippedCurio", Item.class, net.minecraft.world.entity.LivingEntity.class)
                    .invoke(helper, item, player);

            return result instanceof Optional<?> optional && optional.isPresent();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean hasAccessoryCharm(Item item, Player player) {
        return findAccessoryCharmSlot(item, player) != null;
    }

    private static boolean consumeAccessoryCharm(Item item, Player player) {
        AccessorySlot slot = findAccessoryCharmSlot(item, player);
        if (slot == null) {
            return false;
        }

        CompoundTag data = getTwilightPlayerData(player);
        if (data == null) {
            return false;
        }

        data.put(CHARM_STACK_TAG, slot.container().getItem(slot.index()).save(player.registryAccess()));
        slot.consume();
        return true;
    }

    private static AccessorySlot findAccessoryCharmSlot(Item item, Player player) {
        if (!Platform.isModLoaded("accessories")) {
            return null;
        }

        try {
            Class<?> capabilityClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            Object optionalResult = capabilityClass.getMethod("getOptionally", net.minecraft.world.entity.LivingEntity.class).invoke(null, player);
            if (!(optionalResult instanceof Optional<?> optional) || optional.isEmpty()) {
                return null;
            }

            Object capability = optional.get();
            Object containersResult = capability.getClass().getMethod("getContainers").invoke(capability);
            if (!(containersResult instanceof Map<?, ?> containers)) {
                return null;
            }

            for (Object container : containers.values()) {
                AccessorySlot main = findAccessoryCharmSlot(item, container, (Container) container.getClass().getMethod("getAccessories").invoke(container));
                if (main != null) {
                    return main;
                }

                Object cosmetic = container.getClass().getMethod("getCosmeticAccessories").invoke(container);
                if (cosmetic instanceof Container cosmeticContainer) {
                    AccessorySlot cosmeticSlot = findAccessoryCharmSlot(item, container, cosmeticContainer);
                    if (cosmeticSlot != null) {
                        return cosmeticSlot;
                    }
                }
            }
        } catch (ReflectiveOperationException | ClassCastException ignored) {
        }

        return null;
    }

    private static AccessorySlot findAccessoryCharmSlot(Item item, Object owner, Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) {
                return new AccessorySlot(owner, container, i);
            }
        }

        return null;
    }

    private static CompoundTag getTwilightPlayerData(Player player) {
        try {
            Class<?> charmEvents = Class.forName("twilightforest.events.CharmEvents");
            Object result = charmEvents.getMethod("getPlayerData", Player.class).invoke(null, player);
            return result instanceof CompoundTag tag ? tag : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private record AccessorySlot(Object owner, Container container, int index) {
        void consume() {
            var stack = container.getItem(index);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                container.setChanged();
                try {
                    Method method = owner.getClass().getMethod("markChanged");
                    method.invoke(owner);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
    }
}
