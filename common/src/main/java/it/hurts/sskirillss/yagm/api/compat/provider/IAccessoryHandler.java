package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IAccessoryHandler {

    /**
     * Checks if the accessory mod is loaded.
     */
    boolean isModLoaded();

    /**
     * Gets the mod name (for logs).
     */
    String getModName();

    /**
     * Collects all items from the player's accessory slots.
     * Called when the player dies BEFORE clearing the inventory.
     *
     * @param player
     * @return map: slot key -> item (e.g. "ring/0", "necklace/cosmetic/1")
     */
    Map<String, ItemStack> collectAccessories(ServerPlayer player);

    /**
     * Clears all player accessory slots.
     * Called after collecting items.
     *
     * @param player
     */
    void clearAccessories(ServerPlayer player);

    /**
     * Restores items to accessory slots.
     * If the original slot is occupied, the item goes to inventory or is dropped.
     *
     * @param player
     * @param accessories map: slot key -> item
     * @param dropIfFull if true, drop items that didn't fit
     */
    void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull);

    /**
     * Сохраняет аксессуары в NBT тег.
     *
     * @param accessories карта аксессуаров
     * @param registryAccess доступ к реестрам
     * @return NBT тег с сохранёнными данными
     */
    CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess);

    /**
     * Loads accessories from an NBT tag.
     *
     * @param tag NBT tag
     * @param registryAccess registry access
     * @return map: slot key -> item
     */
    Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess);

    /**
     * Converts the accessory map into a flat list (for cost calculations, etc.)
     */
    default NonNullList<ItemStack> toList(Map<String, ItemStack> accessories) {
        NonNullList<ItemStack> list = NonNullList.create();
        for (ItemStack stack : accessories.values()) {
            if (!stack.isEmpty()) {
                list.add(stack.copy());
            }
        }
        return list;
    }

    /**
     * Проверяет, может ли предмет быть экипирован как аксессуар.
     */
    boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack);

    /**
     *Attempts to equip an item into the first available available slot.
     *
     * @return true if successfully equipped
     */
    boolean tryEquipAccessory(ServerPlayer player, ItemStack stack);
}