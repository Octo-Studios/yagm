package it.hurts.sskirillss.yagm.neoforge.compat.cosmeticarmor.slot;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

public enum CosmeticArmorSlot implements StringRepresentable {
    FEET(0, EquipmentSlot.FEET),
    LEGS(1, EquipmentSlot.LEGS),
    CHEST(2, EquipmentSlot.CHEST),
    HEAD(3, EquipmentSlot.HEAD);

    public static final Codec<CosmeticArmorSlot> CODEC = StringRepresentable.fromEnum(CosmeticArmorSlot::values);

    private final int index;

    private final EquipmentSlot equipmentSlot;

    CosmeticArmorSlot(int index, EquipmentSlot equipmentSlot) {
        this.index = index;
        this.equipmentSlot = equipmentSlot;
    }

    public static CosmeticArmorSlot getStack(ItemStack stack) {
        Equipable equipable = Equipable.get(stack);

        return equipable == null ? null : getArmorSlot(equipable.getEquipmentSlot());
    }

    private static CosmeticArmorSlot getArmorSlot(EquipmentSlot equipmentSlot) {
        for (CosmeticArmorSlot slot : values()) {
            if (slot.equipmentSlot == equipmentSlot) {
                return slot;
            }
        }

        return null;
    }

    public int index() {
        return index;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
