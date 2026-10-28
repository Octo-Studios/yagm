package it.hurts.sskirillss.yagm.neoforge.compat.curios;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotAttribute;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
                    accessories.put(slotType + "/" + i, stack.copy());
                }
            }

            IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
            for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                ItemStack stack = cosmeticStacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    accessories.put(slotType + "/cosmetic/" + i, stack.copy());
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

        ICuriosItemHandler curiosHandler = curiosOpt.get();
        List<Map.Entry<String, ItemStack>> remaining = new ArrayList<>(accessories.entrySet());

        restoreOriginalSlots(player, curiosHandler, remaining);

        boolean equipped;
        do {
            equipped = false;

            Iterator<Map.Entry<String, ItemStack>> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                ItemStack clean = withoutSlotData(iterator.next().getValue());
                if (tryEquipAccessory(player, clean)) {
                    iterator.remove();
                    equipped = true;
                }
            }
        } while (equipped && !remaining.isEmpty());

        for (Map.Entry<String, ItemStack> entry : remaining) {
            getInventoryOrDrop(player, withoutSlotData(entry.getValue()), dropIfFull);
        }
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

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

            IDynamicStackHandler stacks = stacksHandler.getStacks();

            for (int i = 0; i < stacks.getSlots(); i++) {
                if (stacks.getStackInSlot(i).isEmpty()) {
                    SlotContext context = new SlotContext(slotType, player, i, false, true);
                    if (CuriosApi.isStackValid(context, stack)) {
                        ItemStack copy = stack.copy();
                        stacks.setStackInSlot(i, copy);
                        if (curiosOpt.get().isSlotActive(slotType, i)) {
                            applySlotModifiers(curiosOpt.get(), context, copy);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void restoreOriginalSlots(ServerPlayer player, ICuriosItemHandler curiosHandler, List<Map.Entry<String, ItemStack>> remaining) {
        boolean restored;

        do {
            restored = false;

            Iterator<Map.Entry<String, ItemStack>> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, ItemStack> entry = iterator.next();
                ItemStack stack = entry.getValue();

                if (stack.isEmpty()) {
                    iterator.remove();
                    continue;
                }

                CurioSlotData.SlotInfo slotData = parseSlotKey(entry.getKey());
                if (slotData == null) {
                    slotData = stack.get(CurioSlotData.CURIO_SLOT_DATA.get());
                }

                if (slotData == null || !slotData.wasEquipped()) {
                    continue;
                }

                ICurioStacksHandler stacksHandler = curiosHandler.getCurios().get(slotData.slotType());

                if (stacksHandler == null) {
                    continue;
                }

                IDynamicStackHandler target = slotData.isCosmetic() ? stacksHandler.getCosmeticStacks() : stacksHandler.getStacks();

                int index = slotData.slotIndex();

                if (index < 0 || index >= target.getSlots() || !target.getStackInSlot(index).isEmpty()) {
                    continue;
                }

                ItemStack clean = withoutSlotData(stack);

                target.setStackInSlot(index, clean);

                if (!slotData.isCosmetic() && curiosHandler.isSlotActive(slotData.slotType(), index)) {
                    SlotContext context = new SlotContext(slotData.slotType(), player, index, false, true);
                    applySlotModifiers(curiosHandler, context, clean);
                }

                iterator.remove();

                restored = true;
            }
        } while (restored && !remaining.isEmpty());
    }

    private static void applySlotModifiers(ICuriosItemHandler curiosHandler, SlotContext context, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> attributes = CuriosApi.getAttributeModifiers(context, CuriosApi.getSlotId(context), stack);

        Multimap<String, AttributeModifier> slotModifiers = HashMultimap.create();

        for (Holder<Attribute> attribute : attributes.keySet()) {
            if (attribute.value() instanceof SlotAttribute slotAttribute) {
                slotModifiers.putAll(slotAttribute.getIdentifier(), attributes.get(attribute));
            }
        }

        if (!slotModifiers.isEmpty()) {
            curiosHandler.addTransientSlotModifiers(slotModifiers);
        }
    }

    private static CurioSlotData.SlotInfo parseSlotKey(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        String[] parts = key.split("/");

        try {
            if (parts.length == 2) {
                return new CurioSlotData.SlotInfo(parts[0], Integer.parseInt(parts[1]), true, false);
            }

            if (parts.length == 3 && "cosmetic".equals(parts[1])) {
                return new CurioSlotData.SlotInfo(parts[0], Integer.parseInt(parts[2]), true, true);
            }
        } catch (NumberFormatException ignored) {}

        return null;
    }

    private static ItemStack withoutSlotData(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.remove(CurioSlotData.CURIO_SLOT_DATA.get());
        return copy;
    }
}
