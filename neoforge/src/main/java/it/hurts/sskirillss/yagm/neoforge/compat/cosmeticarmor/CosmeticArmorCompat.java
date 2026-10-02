package it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import it.hurts.sskirillss.yagm.api.compat.config.CompatTomlConfig;
import it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.key.SlotKey;
import it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.slot.CosmeticArmorSlot;
import lain.mods.cos.api.CosArmorAPI;
import lain.mods.cos.api.inventory.CAStacksBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class CosmeticArmorCompat extends BaseAccessoryCompat {

    @Override
    public boolean isModLoaded() {
        return Platform.isModLoaded("cosmeticarmorreworked");
    }

    @Override
    public String getModName() {
        return "CosmeticArmorReworked";
    }

    @Override
    protected String getNbtTag() {
        return "CosmeticArmorData";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        if (isKeepThroughDeathEnabled()) {
            return Map.of();
        }

        Map<String, ItemStack> accessories = new HashMap<>();

        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());

        for (CosmeticArmorSlot slot : CosmeticArmorSlot.values()) {

            if (slot.index() >= stacks.getSlots()) {
                continue;
            }

            ItemStack stack = stacks.getStackInSlot(slot.index());

            if (!stack.isEmpty()) {
                accessories.put(new SlotKey(slot, stacks.isSkinArmor(slot.index())).serializedName(), stack.copy());
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        if (isKeepThroughDeathEnabled()) {
            return;
        }

        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());

        for (CosmeticArmorSlot slot : CosmeticArmorSlot.values()) {
            if (slot.index() < stacks.getSlots()) {
                stacks.setStackInSlot(slot.index(), ItemStack.EMPTY);

                stacks.setSkinArmor(slot.index(), false);
            }
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        restoreAccessories(player, accessories, dropIfFull, null, null);
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull, Level level, BlockPos dropPos) {
        if (accessories.isEmpty()) {
            return;
        }

        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) {
                continue;
            }

            SlotKey slotKey = SlotKey.byName(entry.getKey());

            if (slotKey != null && restoreSlot(stacks, slotKey.slot(), stack, slotKey.skinArmor())) {
                continue;
            }

            if (!tryEquipAccessory(player, stack)) {
                if (level != null && dropPos != null) {
                    getInventoryOrDrop(player, stack, dropIfFull, level, dropPos);
                } else {
                    getInventoryOrDrop(player, stack, dropIfFull);
                }
            }
        }
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        return CosmeticArmorSlot.getStack(stack) != null;
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        CAStacksBase stacks = CosArmorAPI.getCAStacks(player.getUUID());

        CosmeticArmorSlot slot = CosmeticArmorSlot.getStack(stack);

        return slot != null && restoreSlot(stacks, slot, stack, false);
    }

    private static boolean restoreSlot(CAStacksBase stacks, CosmeticArmorSlot slot, ItemStack stack, boolean skinArmor) {
        if (slot.index() >= stacks.getSlots() || !stacks.getStackInSlot(slot.index()).isEmpty()) {
            return false;
        }

        stacks.setStackInSlot(slot.index(), stack.copy());
        stacks.setSkinArmor(slot.index(), skinArmor);
        return true;
    }

    private static boolean isKeepThroughDeathEnabled() {
        return CompatTomlConfig.readBoolean("cosmeticarmorreworked" + "-common.toml", "CosArmorKeepThroughDeath", false);
    }
}
