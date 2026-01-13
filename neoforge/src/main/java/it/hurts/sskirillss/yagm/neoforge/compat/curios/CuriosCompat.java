package it.hurts.sskirillss.yagm.neoforge.compat.curios;


import it.hurts.sskirillss.yagm.YAGMCommon;
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

/**
 * Handler for Curios mod integration.
 * Based on Leclowndu93150's Corpse-Gravestone-Curios-Compat implementation.
 *
 * Key difference: Slot data is attached directly to ItemStacks via DataComponents,
 * which persists through NBT serialization and allows proper restoration to original slots.
 */
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
                    // Attach slot data directly to the ItemStack
                    CurioSlotData.SlotInfo slotData = new CurioSlotData.SlotInfo(slotType, i, true, false);
                    stackCopy.set(CurioSlotData.CURIO_SLOT_DATA.get(), slotData);
                    
                    // Verify slot data was attached
                    CurioSlotData.SlotInfo attachedData = stackCopy.get(CurioSlotData.CURIO_SLOT_DATA.get());
                    
                    // Key format: "slotType/index" e.g. "ring/0", "necklace/1"
                    String key = slotType + "/" + i;
                    accessories.put(key, stackCopy);
                    YAGMCommon.LOGGER.info("Collected accessory: {} from slot: {} at index {} (slotData attached: {})",
                        stack.getItem(), slotType, i, attachedData != null);
                }
            }

            // Collect cosmetic slots
            IDynamicStackHandler cosmeticStacks = stacksHandler.getCosmeticStacks();
            for (int i = 0; i < cosmeticStacks.getSlots(); i++) {
                ItemStack stack = cosmeticStacks.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemStack stackCopy = stack.copy();
                    // Attach slot data directly to the ItemStack
                    CurioSlotData.SlotInfo slotData = new CurioSlotData.SlotInfo(slotType, i, true, true);
                    stackCopy.set(CurioSlotData.CURIO_SLOT_DATA.get(), slotData);
                    
                    // Verify slot data was attached
                    CurioSlotData.SlotInfo attachedData = stackCopy.get(CurioSlotData.CURIO_SLOT_DATA.get());
                    
                    // Key format: "slotType/cosmetic/index" e.g. "ring/cosmetic/0"
                    String key = slotType + "/cosmetic/" + i;
                    accessories.put(key, stackCopy);
                    YAGMCommon.LOGGER.info("Collected cosmetic accessory: {} from slot: {} at index {} (slotData attached: {})",
                        stack.getItem(), slotType, i, attachedData != null);
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
        YAGMCommon.LOGGER.info("[Curios] Starting restoration for player: {} with {} accessories",
            player.getName().getString(), accessories.size());
        
        if (accessories.isEmpty()) {
            YAGMCommon.LOGGER.warn("[Curios] No accessories to restore!");
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
        YAGMCommon.LOGGER.info("[Curios] Found {} slot types available for restoration", curios.size());

        int restoredCount = 0;
        int failedCount = 0;
        
        for (ItemStack stack : accessories.values()) {
            if (stack.isEmpty()) continue;

            // Read slot data from the ItemStack's DataComponent
            CurioSlotData.SlotInfo slotData = stack.get(CurioSlotData.CURIO_SLOT_DATA.get());
            
            if (slotData != null && slotData.wasEquipped()) {
                // Try to restore to original slot using attached data
                ICurioStacksHandler stacksHandler = curios.get(slotData.slotType());
                if (stacksHandler != null) {
                    IDynamicStackHandler targetStacks = slotData.isCosmetic()
                            ? stacksHandler.getCosmeticStacks()
                            : stacksHandler.getStacks();
                    
                    int slotIndex = slotData.slotIndex();
                    if (slotIndex >= 0 && slotIndex < targetStacks.getSlots()) {
                        ItemStack existing = targetStacks.getStackInSlot(slotIndex);
                        if (existing.isEmpty()) {
                            // Clean the stack and place it
                            ItemStack cleanStack = stack.copy();
                            cleanStack.remove(CurioSlotData.CURIO_SLOT_DATA.get());
                            targetStacks.setStackInSlot(slotIndex, cleanStack);
                            YAGMCommon.LOGGER.info("[Curios] Restored {} to slot {} at index {}",
                                stack.getItem(), slotData.slotType(), slotIndex);
                            restoredCount++;
                            continue;
                        } else {
                            YAGMCommon.LOGGER.warn("[Curios] Slot {} at index {} is occupied",
                                slotData.slotType(), slotIndex);
                        }
                    } else {
                        YAGMCommon.LOGGER.warn("[Curios] Invalid slot index: {} for slot type {}", slotIndex, slotData.slotType());
                    }
                } else {
                    YAGMCommon.LOGGER.warn("[Curios] No stacks handler for slot type: {}", slotData.slotType());
                }
            } else {
                YAGMCommon.LOGGER.warn("[Curios] No slot data for item: {}", stack.getItem());
                failedCount++;
            }

            // Slot was occupied or no slot data, try to equip elsewhere
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
        
        YAGMCommon.LOGGER.info("[Curios] Restoration complete: {} restored, {} failed", restoredCount, failedCount);
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
        YAGMCommon.LOGGER.info("[Curios] Loading accessories from NBT");
        Map<String, ItemStack> accessories = new HashMap<>();

        if (!tag.contains(TAG_ACCESSORIES, Tag.TAG_LIST)) {
            YAGMCommon.LOGGER.warn("[Curios] No accessories tag found in NBT");
            return accessories;
        }

        ListTag itemsList = tag.getList(TAG_ACCESSORIES, Tag.TAG_COMPOUND);
        YAGMCommon.LOGGER.info("[Curios] Found {} items in NBT", itemsList.size());

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            String slotKey = itemTag.getString(TAG_SLOT_KEY);
            ItemStack stack = ItemStack.parseOptional(registryAccess, itemTag.getCompound(TAG_ITEM));

            if (!stack.isEmpty() && !slotKey.isEmpty()) {
                accessories.put(slotKey, stack);
                // Log to verify slot data is preserved after NBT deserialization
                CurioSlotData.SlotInfo slotData = stack.get(CurioSlotData.CURIO_SLOT_DATA.get());
                if (slotData != null) {
                    YAGMCommon.LOGGER.info("[Curios] Loaded {} from slot {} with data: type={}, index={}, cosmetic={}",
                        stack.getItem(), slotKey, slotData.slotType(), slotData.slotIndex(), slotData.isCosmetic());
                } else {
                    YAGMCommon.LOGGER.warn("[Curios] Loaded {} from slot {} WITHOUT slot data!", stack.getItem(), slotKey);
                }
            }
        }

        YAGMCommon.LOGGER.info("[Curios] Loaded {} accessories from NBT", accessories.size());
        return accessories;
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        // Check if item has any valid curio slots
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

        // Get valid slot types for this item
        var slotTypes = CuriosApi.getItemStackSlots(stack, player);
        if (slotTypes == null || slotTypes.isEmpty()) return false;

        for (String slotType : slotTypes.keySet()) {
            ICurioStacksHandler stacksHandler = curios.get(slotType);
            if (stacksHandler == null) continue;

            // Check if item is valid for this slot
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