package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GravePayloadUtils {
    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    private static final String[] LIST_KEYS = {
            KEYS.getMainInventory(), KEYS.getArmorInventory(), KEYS.getOffhandInventory(), KEYS.getDroppedItems()
    };

    private static final String[] COMPOUND_KEYS = {
            KEYS.getAccessories(), KEYS.getBackpacks()
    };

    private static final String[] ALL_KEYS = {
            KEYS.getMainInventory(), KEYS.getArmorInventory(), KEYS.getOffhandInventory(), KEYS.getDroppedItems(),
            KEYS.getAccessories(), KEYS.getBackpacks(), KEYS.getTotalExperience()
    };

    public static boolean hasRecoverablePayload(CompoundTag data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        for (String key : LIST_KEYS) {
            if (data.get(key) instanceof ListTag list && !list.isEmpty()) {
                return true;
            }
        }

        for (String key : COMPOUND_KEYS) {
            if (data.get(key) instanceof CompoundTag compound && !compound.isEmpty()) {
                return true;
            }
        }

        return data.contains(KEYS.getTotalExperience(), Tag.TAG_LONG) && data.getLong(KEYS.getTotalExperience()) > 0;
    }

    public static CompoundTag copyRecoverablePayload(CompoundTag source) {
        CompoundTag result = new CompoundTag();
        if (source == null) {
            return result;
        }

        for (String key : ALL_KEYS) {
            Tag value = source.get(key);
            if (value != null) {
                result.put(key, value.copy());
            }
        }

        return result;
    }
}
