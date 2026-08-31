package it.hurts.sskirillss.yagm.fabric.compat.backpack;

import com.tiviacz.travelersbackpack.TravelersBackpack;
import com.tiviacz.travelersbackpack.component.ComponentUtils;
import com.tiviacz.travelersbackpack.component.ITravelersBackpack;
import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackEntry;
import it.hurts.sskirillss.yagm.api.compat.backpack.BaseBackpackCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FabricTravelersBackpackCompat extends BaseBackpackCompat {

    @Override
    public String getModName() {
        return "travelersbackpack";
    }

    @Override
    public boolean isModLoaded() {
        return Platform.isModLoaded("travelersbackpack") && !intloaded();
    }

    @Override
    public List<BackpackEntry> collectBackpacks(ServerPlayer player) {
        if (!ComponentUtils.isWearingBackpack(player)) {
            return List.of();
        }

        ItemStack backpack = ComponentUtils.getWearingBackpack(player);
        if (backpack.isEmpty() || hasMatchingInventoryStack(player, backpack)) {
            return List.of();
        }

        return List.of(new BackpackEntry("wearable", backpack.copy()));
    }

    @Override
    public void clearBackpacks(ServerPlayer player) {
        ComponentUtils.getComponent(player).ifPresent(ITravelersBackpack::removeWearable);
    }

    @Override
    protected boolean restoreLeft(ServerPlayer player, BackpackEntry backpack) {
        if (!"wearable".equals(backpack.slotKey())) {
            return false;
        }

        if (hasMatchingInventoryStack(player, backpack.stack())) {
            return true;
        }

        ItemStack wearingBackpack = ComponentUtils.getWearingBackpack(player);

        if (ItemStack.matches(wearingBackpack, backpack.stack())) {
            return true;
        }

        if (!wearingBackpack.isEmpty()) {
            return false;
        }

        ComponentUtils.equipBackpack(player, backpack.stack().copy());
        return !ComponentUtils.getWearingBackpack(player).isEmpty();
    }

    private boolean intloaded() {
        if (!TravelersBackpack.enableIntegration()) {
            return false;
        }

        return Platform.isModLoaded("accessories") || Platform.isModLoaded("trinkets");
    }
}
