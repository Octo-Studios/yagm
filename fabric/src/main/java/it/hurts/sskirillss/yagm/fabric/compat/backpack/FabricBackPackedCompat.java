package it.hurts.sskirillss.yagm.fabric.compat.backpack;

import com.mrcrayfish.backpacked.BackpackHelper;
import com.mrcrayfish.backpacked.common.augment.AugmentHandler;
import com.mrcrayfish.backpacked.common.augment.impl.RecallAugment;
import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackEntry;
import it.hurts.sskirillss.yagm.api.compat.backpack.BaseBackpackCompat;
import it.hurts.sskirillss.yagm.api.compat.config.CompatTomlConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class FabricBackPackedCompat extends BaseBackpackCompat {

    @Override
    public String getModName() {
        return "backpacked";
    }

    @Override
    public boolean isModLoaded() {
        return Platform.isModLoaded("backpacked");
    }

    @Override
    public List<BackpackEntry> collectBackpacks(ServerPlayer player) {
        NonNullList<ItemStack> backpacks = BackpackHelper.getBackpacks(player);
        List<BackpackEntry> entries = new ArrayList<>();
        boolean keepOnDeath = isEnabled();

        for (int i = 0; i < backpacks.size(); i++) {
            ItemStack backpack = backpacks.get(i);
            if (!backpack.isEmpty() && shouldCapture(player, i, backpack, keepOnDeath)) {
                entries.add(new BackpackEntry(Integer.toString(i), backpack.copy()));
            }
        }

        return entries;
    }

    @Override
    public void clearBackpacks(ServerPlayer player) {
        if (isEnabled()) {
            return;
        }

        NonNullList<ItemStack> backpacks = BackpackHelper.getBackpacks(player);

        for (int i = 0; i < backpacks.size(); i++) {
            ItemStack backpack = backpacks.get(i);
            if (!backpack.isEmpty()) {
                BackpackHelper.setBackpackStack(player, ItemStack.EMPTY, i);
            }
        }
    }

    @Override
    protected boolean restoreLeft(ServerPlayer player, BackpackEntry backpack) {
        int index = parseIndex(backpack.slotKey());
        if (index < 0 || !BackpackHelper.getBackpackStack(player, index).isEmpty()) {
            return false;
        }

        return BackpackHelper.setBackpackStack(player, backpack.stack().copy(), index);
    }

    @Override
    protected boolean restoreRight(ServerPlayer player, BackpackEntry backpack) {
        return BackpackHelper.equipBackpack(player, backpack.stack().copy());
    }

    private static int parseIndex(String slotKey) {
        if (slotKey == null || slotKey.isEmpty()) {
            return -1;
        }

        try {
            return Integer.parseInt(slotKey);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean shouldCapture(ServerPlayer player, int index, ItemStack backpack, boolean keepOnDeath) {
        return !keepOnDeath && !tryRecall(player, index, backpack);
    }

    private static boolean isEnabled() {
        return CompatTomlConfig.readBoolean("backpacked.backpack.toml", "keepOnDeath", false);
    }

    private static boolean tryRecall(ServerPlayer player, int index, ItemStack backpack) {
        RecallAugment recall = BackpackHelper.findAugment(backpack, RecallAugment.TYPE);
        return recall != null && recall.shelfKey().isPresent() && AugmentHandler.recallBackpack(player, index, backpack, recall);
    }
}
