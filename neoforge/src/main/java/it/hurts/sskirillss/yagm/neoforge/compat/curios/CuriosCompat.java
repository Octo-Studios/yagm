package it.hurts.sskirillss.yagm.neoforge.compat.curios;


import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import it.hurts.sskirillss.yagm.neoforge.compat.curios.slot.CurioSlotData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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


public class CuriosCompat implements IAccessoryHandler {

    private static final String TAG_ACCESSORIES = "CuriosAccessories";
    private static final String TAG_SLOT_KEY = "SlotKey";
    private static final String TAG_ITEM = "Item";

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("curios");
    }

    @Override
    public String getModName() {
        return "Curios";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) {
            return accessories;
        }

        ICuriosItemHandler curiosHandler = curiosOpt.get();
        Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();

        for (Map.Entry<String, ICurioStacksHandler> entry : curios.entrySet()) {
            String slotType = entry.getKey();
            ICurioStacksHandler stacksHandler = entry.getValue();


            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack stackCopy = stack.copy();
                    CurioSlotData.SlotInfo slotData = new CurioSlotData.SlotInfo(slotType, i, true, false);
                    stackCopy.set(CurioSlotData.CURIO_SLOT_DATA.get(), slotData);

                    String key = slotType + "/" + i;
                    accessories.put(key, stackCopy);

                }
            }

            IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
            for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                ItemStack stack = cosmeticStacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack stackCopy = stack.copy();
                    CurioSlotData.SlotInfo slotData = new CurioSlotData.SlotInfo(slotType, i, true, true);
                    stackCopy.set(CurioSlotData.CURIO_SLOT_DATA.get(), slotData);


                    String key = slotType + "/cosmetic/" + i;
                    accessories.put(key, stackCopy);
                }
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) {
            return;
        }

        ICuriosItemHandler curiosHandler = curiosOpt.get();
        Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();

        for (ICurioStacksHandler stacksHandler : curios.values()) {
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
        if (accessories.isEmpty()) {
            return;
        }

        Optional<ICuriosItemHandler> curiosOpt = CuriosApi.getCuriosInventory(player);
        if (curiosOpt.isEmpty()) {
            for (ItemStack stack : accessories.values()) {
                if (!stack.isEmpty()) {
                    ItemStack cleanStack = stack.copy();
                    cleanStack.remove(CurioSlotData.CURIO_SLOT_DATA.get());
                    if (!player.getInventory().add(cleanStack)) {
                        if (dropIfFull) {
                            player.drop(cleanStack, false);
                        }
                    }
                }
            }
            return;
        }

        ICuriosItemHandler curiosHandler = curiosOpt.get();
        Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();

        for (ItemStack stack : accessories.values()) {
            if (stack.isEmpty()) continue;

            CurioSlotData.SlotInfo slotData = stack.get(CurioSlotData.CURIO_SLOT_DATA.get());
            
            if (slotData != null && slotData.wasEquipped()) {
                ICurioStacksHandler stacksHandler = curios.get(slotData.slotType());
                if (stacksHandler != null) {
                    IDynamicStackHandler targetStacks = slotData.isCosmetic() ? stacksHandler.getCosmeticStacks() : stacksHandler.getStacks();

                    int slotIndex = slotData.slotIndex();
                    if (slotIndex >= 0 && slotIndex < targetStacks.getSlots()) {
                        ItemStack existing = targetStacks.getStackInSlot(slotIndex);
                        if (existing.isEmpty()) {
                            ItemStack cleanStack = stack.copy();
                            cleanStack.remove(CurioSlotData.CURIO_SLOT_DATA.get());
                            targetStacks.setStackInSlot(slotIndex, cleanStack);
                            continue;
                        }
                    }
                }
            }


            ItemStack cleanStack = stack.copy();
            cleanStack.remove(CurioSlotData.CURIO_SLOT_DATA.get());
            
            if (!tryEquipAccessory(player, cleanStack)) {
                if (!player.getInventory().add(cleanStack)) {
                    if (dropIfFull) {
                        player.drop(cleanStack, false);
                    }
                }
            }
        }
    }

    @Override
    public CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            CompoundTag itemTag = new CompoundTag();
            itemTag.putString(TAG_SLOT_KEY, entry.getKey());
            itemTag.put(TAG_ITEM, entry.getValue().save(registryAccess));
            itemsList.add(itemTag);
        }

        tag.put(TAG_ACCESSORIES, itemsList);
        return tag;
    }

    @Override
    public Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, ItemStack> accessories = new HashMap<>();

        if (!tag.contains(TAG_ACCESSORIES, Tag.TAG_LIST)) {
            return accessories;
        }

        ListTag itemsList = tag.getList(TAG_ACCESSORIES, Tag.TAG_COMPOUND);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            String slotKey = itemTag.getString(TAG_SLOT_KEY);
            ItemStack stack = ItemStack.parseOptional(registryAccess, itemTag.getCompound(TAG_ITEM));

            if (!stack.isEmpty() && !slotKey.isEmpty()) {
                accessories.put(slotKey, stack);
            }
        }
        return accessories;
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

        ICuriosItemHandler curiosHandler = curiosOpt.get();
        Map<String, ICurioStacksHandler> curios = curiosHandler.getCurios();

        var slotTypes = CuriosApi.getItemStackSlots(stack, player);
        if (slotTypes == null || slotTypes.isEmpty()) return false;

        for (String slotType : slotTypes.keySet()) {
            ICurioStacksHandler stacksHandler = curios.get(slotType);
            if (stacksHandler == null) continue;

            SlotContext context = new SlotContext(slotType, player, 0, false, true);
            if (!CuriosApi.isStackValid(context, stack)) {
                continue;
            }

            IDynamicStackHandler stacks = stacksHandler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (stacks.getStackInSlot(i).isEmpty()) {
                    stacks.setStackInSlot(i, stack.copy());
                    return true;
                }
            }
        }

        return false;
    }
}