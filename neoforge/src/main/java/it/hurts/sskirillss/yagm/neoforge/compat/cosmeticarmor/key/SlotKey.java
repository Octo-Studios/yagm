package it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.key;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.slot.CosmeticArmorSlot;


public record SlotKey(CosmeticArmorSlot slot, boolean skinArmor) {

    private static final Codec<SlotKey> CODEC = Codec.STRING.comapFlatMap(SlotKey::decode, SlotKey::serializedName);

    public static SlotKey byName(String name) {
        return CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(name)).result().orElse(null);
    }

    private static DataResult<SlotKey> decode(String name) {
        if (name == null || name.isEmpty()) {
            return DataResult.error(() -> "Cosmetic Armor slot key is empty");
        }

        String[] parts = name.split("/", -1);

        CosmeticArmorSlot slot = CosmeticArmorSlot.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive(parts[0])).result().orElse(null);

        if (slot == null) {
            return DataResult.error(() -> "Unknown Cosmetic Armor slot: " + parts[0]);
        }

        if (parts.length == 1) {
            return DataResult.success(new SlotKey(slot, false));
        }

        if (parts.length == 2 && "skin".equals(parts[1])) {
            return DataResult.success(new SlotKey(slot, true));
        }

        return DataResult.error(() -> "Invalid Cosmetic Armor slot key: " + name);
    }

    public String serializedName() {
        return slot.getSerializedName() + (skinArmor ? "/skin" : "");
    }
}
