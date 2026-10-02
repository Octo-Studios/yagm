package it.hurts.sskirillss.yagm.api.compat.backpack.handlers;

import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackEntry;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackRestoreContext;
import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackRestoreResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;


public interface IBackpackHandler {

    String getModName();

    boolean isModLoaded();

    List<BackpackEntry> collectBackpacks(ServerPlayer player);

    void clearBackpacks(ServerPlayer player);

    CompoundTag saveToNBT(List<BackpackEntry> backpacks, RegistryAccess registryAccess);

    List<BackpackEntry> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess);

    BackpackRestoreResult restoreBackpack(ServerPlayer player, BackpackEntry backpack, BackpackRestoreContext context);
}
