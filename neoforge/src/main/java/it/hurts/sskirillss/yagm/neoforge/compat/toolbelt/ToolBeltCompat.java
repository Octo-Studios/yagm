package it.hurts.sskirillss.yagm.neoforge.compat.toolbelt;

import dev.architectury.platform.Platform;
import dev.gigaherz.toolbelt.ConfigData;
import dev.gigaherz.toolbelt.slot.BeltAttachment;
import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.Map;

public class ToolBeltCompat extends BaseAccessoryCompat {

    @Override
    public boolean isModLoaded() {
        return Platform.isModLoaded("toolbelt");
    }

    @Override
    public String getModName() {
        return "ToolBelt";
    }

    @Override
    protected String getNbtTag() {
        return "ToolBeltData";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        if (!ConfigData.customBeltSlotEnabled) {
            return Map.of();
        }

        var stack = BeltAttachment.get(player).getContents();
        if (stack.isEmpty()) {
            return Map.of();
        }

        var vanishing = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.VANISHING_CURSE);
        if (stack.getEnchantmentLevel(vanishing) > 0) {
            return Map.of();
        }

        return Map.of("belt", stack.copy());
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        if (ConfigData.customBeltSlotEnabled) {
            BeltAttachment.get(player).setContents(ItemStack.EMPTY);
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        restoreAccessories(player, accessories, dropIfFull, null, null);
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull, Level level, BlockPos dropPos) {
        for (var stack : accessories.values()) {
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
        return ConfigData.customBeltSlotEnabled && BeltAttachment.get(player).canEquip(stack);
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (!canEquipAsAccessory(player, stack)) {
            return false;
        }

        var attachment = BeltAttachment.get(player);
        if (!attachment.getContents().isEmpty()) {
            return false;
        }

        attachment.setContents(stack.copy());
        return true;
    }
}
