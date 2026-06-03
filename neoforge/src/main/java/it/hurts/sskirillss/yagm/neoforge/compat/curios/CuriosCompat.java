package it.hurts.sskirillss.yagm.neoforge.compat.curios;

import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CuriosCompat extends BaseAccessoryCompat {

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("curios");
    }

    @Override
    public String getModName() {
        return "Curios";
    }

    @Override
    protected String getNbtTag() {
        return "CuriosData";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) return accessories;

        for (Map.Entry<String, ICurioStacksHandler> entry : curiosOpt.get().getCurios().entrySet()) {
            String slotType = entry.getKey();
            ICurioStacksHandler stacksHandler = entry.getValue();

            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.set(CurioSlotData.CURIO_SLOT_DATA.get(), new CurioSlotData.SlotInfo(slotType, i, true, false));
                    accessories.put(slotType + "/" + i, copy);
                }
            }

            IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
            for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                ItemStack stack = cosmeticStacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.set(CurioSlotData.CURIO_SLOT_DATA.get(), new CurioSlotData.SlotInfo(slotType, i, true, true));
                    accessories.put(slotType + "/cosmetic/" + i, copy);
                }
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) return;

        for (ICurioStacksHandler stacksHandler : curiosOpt.get().getCurios().values()) {
            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
            }

            IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
            for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                cosmeticStacks.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        if (accessories.isEmpty()) return;

        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) {
            getInventoryOrDrop(player, accessories.values().stream().map(CuriosCompat::withoutSlotData).toList(), dropIfFull);
            return;
        }

        Map<String, ICurioStacksHandler> curios = curiosOpt.get().getCurios();

        for (ItemStack stack : accessories.values()) {
            if (stack.isEmpty()) continue;

            CurioSlotData.SlotInfo slotData = stack.get(CurioSlotData.CURIO_SLOT_DATA.get());

            if (slotData != null && slotData.wasEquipped()) {
                ICurioStacksHandler stacksHandler = curios.get(slotData.slotType());
                if (stacksHandler != null) {
                    IDynamicStackHandler target = slotData.isCosmetic() ? stacksHandler.getCosmeticStacks() : stacksHandler.getStacks();
                    int idx = slotData.slotIndex();

                    if (idx >= 0 && idx < target.getSlots() && target.getStackInSlot(idx).isEmpty()) {
                        target.setStackInSlot(idx, withoutSlotData(stack));
                        continue;
                    }
                }
            }

            ItemStack clean = withoutSlotData(stack);
            if (!tryEquipAccessory(player, clean)) {
                getInventoryOrDrop(player, clean, dropIfFull);
            }
        }
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        var slotTypes = CuriosApi.getItemStackSlots(stack, player);
        return slotTypes != null && !slotTypes.isEmpty();
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) return false;

        var slotTypes = CuriosApi.getItemStackSlots(stack, player);
        if (slotTypes == null || slotTypes.isEmpty()) return false;

        Map<String, ICurioStacksHandler> curios = curiosOpt.get().getCurios();

        for (String slotType : slotTypes.keySet()) {
            ICurioStacksHandler stacksHandler = curios.get(slotType);
            if (stacksHandler == null) continue;

            SlotContext context = new SlotContext(slotType, player, 0, false, true);
            CuriosApi.isStackValid(context, stack);
        }
        return false;
    }

    private static ItemStack withoutSlotData(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.remove(CurioSlotData.CURIO_SLOT_DATA.get());
        return copy;
    }
}
