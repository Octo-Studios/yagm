package it.hurts.sskirillss.yagm.fabric.compat.trinkets;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import lombok.Value;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Accessory handler for the <a href="https://github.com/emilyploszaj/trinkets">Trinkets</a> mod (Fabric only).
 *
 * <p>Slot key format: {@code "group/slot/index"} — e.g. {@code "chest/necklace/0"}.
 */
public class TrinketsCompat extends BaseAccessoryCompat {

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("trinkets");
    }

    @Override
    public String getModName() {
        return "Trinkets";
    }

    @Override
    protected String getNbtTag() {
        return "TrinketsData";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) return accessories;

        for (var groupEntry : componentOpt.get().getInventory().entrySet()) {
            String groupName = groupEntry.getKey();
            for (var slotEntry : groupEntry.getValue().entrySet()) {
                String slotName = slotEntry.getKey();
                TrinketInventory inv = slotEntry.getValue();

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack stack = inv.getItem(i);
                    if (!stack.isEmpty()) {
                        accessories.put(groupName + "/" + slotName + "/" + i, stack.copy());
                    }
                }
            }
        }

        return accessories;
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) return;

        for (var groupEntry : componentOpt.get().getInventory().entrySet()) {
            for (var slotEntry : groupEntry.getValue().entrySet()) {
                TrinketInventory inv = slotEntry.getValue();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        if (accessories.isEmpty()) return;

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) {
            fallbackToInventoryOrDrop(player, accessories.values(), dropIfFull);
            return;
        }

        Map<String, Map<String, TrinketInventory>> inventory = componentOpt.get().getInventory();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            ItemStack stack = entry.getValue();
            if (stack.isEmpty()) continue;

            SlotInfo slotInfo = parseSlotKey(entry.getKey());

            if (slotInfo != null) {
                Map<String, TrinketInventory> group = inventory.get(slotInfo.getGroupName());
                if (group != null) {
                    TrinketInventory inv = group.get(slotInfo.getSlotName());
                    if (inv != null && slotInfo.getIndex() >= 0 && slotInfo.getIndex() < inv.getContainerSize()) {
                        if (inv.getItem(slotInfo.getIndex()).isEmpty()) {
                            inv.setItem(slotInfo.getIndex(), stack.copy());
                            continue;
                        }
                    }
                }
            }

            if (!tryEquipAccessory(player, stack)) {
                fallbackToInventoryOrDrop(player, stack, dropIfFull);
            }
        }
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && TrinketsApi.getTrinket(stack.getItem()) != null;
    }

    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<TrinketComponent> componentOpt = TrinketsApi.getTrinketComponent(player);
        if (componentOpt.isEmpty()) return false;

        for (var groupEntry : componentOpt.get().getInventory().entrySet()) {
            for (var slotEntry : groupEntry.getValue().entrySet()) {
                TrinketInventory inv = slotEntry.getValue();
                Set<ResourceLocation> predicates = inv.getSlotType().getValidatorPredicates();

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).isEmpty() && (predicates.isEmpty() || stack.getItem() instanceof Trinket)) {
                        inv.setItem(i, stack.copy());
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private SlotInfo parseSlotKey(String key) {
        if (key == null || key.isEmpty()) return null;

        String[] parts = key.split("/");
        if (parts.length != 3) return null;

        try {
            return new SlotInfo(parts[0], parts[1], Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Value
    private static class SlotInfo {
        String groupName;
        String slotName;
        int index;
    }
}
