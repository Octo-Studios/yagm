package it.hurts.sskirillss.yagm.neoforge.compat.twilight;

import dev.architectury.platform.Platform;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import twilightforest.compat.curios.CuriosCompat;
import twilightforest.util.TFItemStackUtils;

import java.util.Optional;

public final class TwilightCharmSlotHook {

    private enum Source { NONE, ACCESSORIES, CURIOS }

    private record CharmState(Source source, AccessorySlot slot) {
        static final CharmState NONE = new CharmState(Source.NONE, null);
        static final CharmState CURIOS = new CharmState(Source.CURIOS, null);
        static CharmState of(AccessorySlot slot) {
            return new CharmState(Source.ACCESSORIES, slot);
        }
    }

    private static final ThreadLocal<CharmState> pending = ThreadLocal.withInitial(() -> CharmState.NONE);

    public static boolean consumeEquippedCharm(Item item, Player player) {
        AccessorySlot slot = findAccessoryCharmSlot(item, player);
        if (slot != null) {
            pending.set(CharmState.of(slot));
            return true;
        }
        if (Platform.isModLoaded("curios") && CuriosCompat.findAndConsumeCurio(item, player)) {
            pending.set(CharmState.CURIOS);
            return true;
        }
        pending.set(CharmState.NONE);
        return false;
    }

    public static boolean consumeItemEquipped(Player player, ItemLike itemLike, CompoundTag data, boolean saveCharm) {
        CharmState state = pending.get();

        pending.set(CharmState.NONE);

        return switch (state.source()) {
            case ACCESSORIES -> {
                ItemStack stack = state.slot().container().getItem(state.slot().index()).copy();
                state.slot().consume();
                if (saveCharm) data.put("CharmStack", stack.save(player.registryAccess()));
                yield true;
            }
            case CURIOS -> false;

            case NONE -> {
                if (isCharmEquipped(itemLike.asItem(), player)) yield false;
                yield TFItemStackUtils.consumeInventoryItem(player, itemLike, data, saveCharm);
            }
        };
    }


    private static boolean isCharmEquipped(Item item, Player player) {
        return isCurioEquipped(item, player) || findAccessoryCharmSlot(item, player) != null;
    }

    private static boolean isCurioEquipped(Item item, Player player) {
        if (!Platform.isModLoaded("curios")) return false;

        return CuriosApi.getCuriosInventory(player).map(handler -> {


            for (var stacksHandler : handler.getCurios().values()) {
                IDynamicStackHandler stacks = stacksHandler.getStacks();

                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).is(item)) return true;
                }
            }
            return false;
        }).orElse(false);

    }

    private static AccessorySlot findAccessoryCharmSlot(Item item, Player player) {
        if (!Platform.isModLoaded("accessories")) return null;

        Optional<AccessoriesCapability> cap = AccessoriesCapability.getOptionally(player);


        if (cap.isEmpty()) return null;

        for (AccessoriesContainer c : cap.get().getContainers().values()) {

            AccessorySlot slot = InContainer(item, c, c.getAccessories());

            if (slot != null) return slot;

            slot = InContainer(item, c, c.getCosmeticAccessories());

            if (slot != null) return slot;
        }
        return null;
    }

    private static AccessorySlot InContainer(Item item, AccessoriesContainer owner, Container container) {
        if (container == null) return null;

        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).is(item)) return new AccessorySlot(owner, container, i);
        }

        return null;
    }


    private record AccessorySlot(AccessoriesContainer owner, Container container, int index) {
        void consume() {
            ItemStack stack = container.getItem(index);
            if (stack.isEmpty()) return;
            stack.shrink(1);
            container.setChanged();
            owner.markChanged();
        }
    }
}
