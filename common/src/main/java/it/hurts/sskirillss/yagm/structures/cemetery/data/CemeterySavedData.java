package it.hurts.sskirillss.yagm.structures.cemetery.data;

import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.structures.cemetery.CemeteryManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public class CemeterySavedData extends SavedData {

    private static final String DATA_NAME = YAGMCommon.MODID + "_cemeteries";

    public CemeterySavedData() {}

    public CemeterySavedData(CompoundTag tag, HolderLookup.Provider provider) {
        CemeteryManager.getInstance().load(tag.getCompound("data"), key -> {
            ResourceLocation loc = ResourceLocation.tryParse(key);
            return loc != null ? ResourceKey.create(Registries.DIMENSION, loc) : null;
        });
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("data", CemeteryManager.getInstance().save());
        return tag;
    }

    public static CemeterySavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(CemeterySavedData::new, CemeterySavedData::new, DataFixTypes.LEVEL),
                DATA_NAME
        );
    }

    public static void markDirty(ServerLevel level) {
        get(level).setDirty();
    }
}