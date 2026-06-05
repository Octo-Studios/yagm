package it.hurts.sskirillss.yagm.neoforge.compat.accessories;

import it.hurts.sskirillss.yagm.api.compat.accessories.AccessoriesCompatImpl;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;
import net.minecraft.world.item.ItemStack;

public class AccessoriesCompat extends AccessoriesCompatImpl {

    @Override
    public boolean isModLoaded() {
        return ModList.get().isLoaded("accessories");
    }

    @Override
    public void clearAccessories(ServerPlayer player) {
        super.clearAccessories(player);
        if (!ModList.get().isLoaded("curios")) return;

        CuriosApi.getCuriosInventory(player).ifPresent(handler ->
                handler.getCurios().values().forEach(stacksHandler -> {
                    IDynamicStackHandler stacks = stacksHandler.getStacks();
                    for (int i = 0; i < stacks.getSlots(); i++) {
                        stacks.setStackInSlot(i, ItemStack.EMPTY);
                    }
                })
        );
    }
}
