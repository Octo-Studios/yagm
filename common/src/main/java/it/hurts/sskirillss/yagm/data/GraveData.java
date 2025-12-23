package it.hurts.sskirillss.yagm.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GraveData {
    private UUID ownerUUID;
    private String ownerName;
    private UUID graveId;
    private long deathTime;
    private CompoundTag inventoryData;
    private NonNullList<ItemStack> transientMain;
    private NonNullList<ItemStack> transientArmor;
    private NonNullList<ItemStack> transientOffhand;

}
