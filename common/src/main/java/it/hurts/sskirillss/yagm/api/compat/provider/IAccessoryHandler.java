package it.hurts.sskirillss.yagm.api.compat.provider;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public interface IAccessoryHandler {

    /**
     * Проверяет, загружен ли мод аксессуаров.
     */
    boolean isModLoaded();

    /**
     * Получает название мода (для логов).
     */
    String getModName();

    /**
     * Собирает все предметы из слотов аксессуаров игрока.
     * Вызывается при смерти игрока ПЕРЕД очисткой инвентаря.
     *
     * @param player игрок
     * @return карта: ключ слота -> предмет (например "ring/0", "necklace/cosmetic/1")
     */
    Map<String, ItemStack> collectAccessories(ServerPlayer player);

    /**
     * Очищает все слоты аксессуаров игрока.
     * Вызывается после сбора предметов.
     *
     * @param player игрок
     */
    void clearAccessories(ServerPlayer player);

    /**
     * Восстанавливает предметы в слоты аксессуаров.
     * Если оригинальный слот занят — предмет идёт в инвентарь или дропается.
     *
     * @param player игрок
     * @param accessories карта: ключ слота -> предмет
     * @param dropIfFull если true — дропать предметы которые не влезли
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
     * Загружает аксессуары из NBT тега.
     *
     * @param tag NBT тег
     * @param registryAccess доступ к реестрам
     * @return карта: ключ слота -> предмет
     */
    Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess);

    /**
     * Преобразует карту аксессуаров в плоский список (для подсчёта стоимости и т.д.)
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
     * Пытается экипировать предмет в первый подходящий свободный слот.
     *
     * @return true если успешно экипирован
     */
    boolean tryEquipAccessory(ServerPlayer player, ItemStack stack);
}