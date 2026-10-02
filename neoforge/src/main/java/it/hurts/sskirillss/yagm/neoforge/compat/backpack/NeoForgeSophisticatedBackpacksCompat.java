package it.hurts.sskirillss.yagm.neoforge.compat.backpack;

import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.backpack.handlers.IBackpackItemHandler;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;

public class NeoForgeSophisticatedBackpacksCompat implements IBackpackItemHandler {

    @Override
    public String getModName() {
        return "sophisticatedbackpacks";
    }

    @Override
    public boolean isModLoaded() {
        return Platform.isModLoaded("sophisticatedbackpacks");
    }

    @Override
    public boolean supports(ItemStack stack) {
        return isModLoaded() && stack.getItem() instanceof BackpackItem;
    }

    @Override
    public ItemStack copyForGrave(ItemStack stack) {
        if (supports(stack)) {
            return BackpackWrapper.fromStack(stack).cloneBackpack();
        } else {
            return stack.copy();
        }
    }
}
