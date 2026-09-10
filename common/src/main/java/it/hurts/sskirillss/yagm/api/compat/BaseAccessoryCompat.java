package it.hurts.sskirillss.yagm.api.compat;

import it.hurts.sskirillss.yagm.api.compat.backpack.BackpackLoader;
import it.hurts.sskirillss.yagm.api.compat.provider.IAccessoryHandler;
import net.minecraft.core.BlockPos;
import it.hurts.sskirillss.yagm.nbt.keys.NbtKeys;
import net.minecraft.world.Containers;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class BaseAccessoryCompat implements IAccessoryHandler {

    private static final NbtKeys NBT_KEYS = NbtKeys.INSTANCE;

    protected abstract String getNbtTag();

    @Override
    public CompoundTag saveToNBT(Map<String, ItemStack> accessories, RegistryAccess registryAccess) {
        CompoundTag tag = new CompoundTag();
        ListTag itemsList = new ListTag();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("SlotKey", entry.getKey());
            itemTag.put("Item", BackpackLoader.copyInventoryStack(entry.getValue()).save(registryAccess));
            itemsList.add(itemTag);
        }

        tag.put(getNbtTag(), itemsList);
        return tag;
    }

    @Override
    public Map<String, ItemStack> loadFromNBT(CompoundTag tag, RegistryAccess registryAccess) {
        Map<String, ItemStack> accessories = new HashMap<>();

        if (!tag.contains(getNbtTag(), Tag.TAG_LIST)) {
            return accessories;
        }

        ListTag itemsList = tag.getList(getNbtTag(), Tag.TAG_COMPOUND);

        for (int i = 0; i < itemsList.size(); i++) {
            CompoundTag itemTag = itemsList.getCompound(i);
            String slotKey = itemTag.getString("SlotKey");
            ItemStack stack = ItemStack.parseOptional(registryAccess, itemTag.getCompound("Item"));

            if (!stack.isEmpty() && !slotKey.isEmpty()) {
                accessories.put(slotKey, stack);
            }
        }

        return accessories;
    }

    protected void getInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull) {
        for (ItemStack stack : stacks) {
            getInventoryOrDrop(player, stack, dropIfFull);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull) {
        if (stack.isEmpty()) return;

        ItemStack rem = stack.copy();

        player.getInventory().add(rem);

        if (!rem.isEmpty() && dropIfFull) {
            player.drop(rem, false);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, Iterable<ItemStack> stacks, boolean dropIfFull, Level level, BlockPos dropPos) {
        for (ItemStack stack : stacks) {
            getInventoryOrDrop(player, stack, dropIfFull, level, dropPos);
        }
    }

    protected void getInventoryOrDrop(ServerPlayer player, ItemStack stack, boolean dropIfFull, Level level, BlockPos dropPos) {
        if (stack.isEmpty()) return;

        ItemStack rem = stack.copy();

        player.getInventory().add(rem);
        if (!rem.isEmpty() && dropIfFull) {
            Containers.dropItemStack(level, dropPos.getX() + 0.5, dropPos.getY() + 0.5, dropPos.getZ() + 0.5, rem);
        }
    }

    public static List<ItemStack> parseAccessories(RegistryAccess registry, CompoundTag data, String handlerName) {
        return parseAccessories(registry, data, name -> handlerName != null && handlerName.equals(name));
    }

    public static List<ItemStack> parseAccessoriesOrElse(RegistryAccess registry, CompoundTag data, String handlerName) {
        return parseAccessories(registry, data, name -> handlerName == null || !handlerName.equals(name));
    }

    private static List<ItemStack> parseAccessories(RegistryAccess registry, CompoundTag data, Predicate<String> handlerFilter) {
        if (registry == null || data == null || !data.contains(NBT_KEYS.getAccessories(), Tag.TAG_COMPOUND)) {
            return List.of();
        }

        CompoundTag root = data.getCompound(NBT_KEYS.getAccessories());

        List<ItemStack> accessories = new ArrayList<>();

        for (String handlerName : root.getAllKeys()) {
            if (!handlerFilter.test(handlerName) || !root.contains(handlerName, Tag.TAG_COMPOUND)) {
                continue;
            }

            collectAccessoryItems(registry, root.getCompound(handlerName), accessories);
        }

        return List.copyOf(accessories);
    }

    private static void collectAccessoryItems(RegistryAccess registry, CompoundTag handlerData, List<ItemStack> accessories) {
        for (String key : handlerData.getAllKeys()) {
            if (!handlerData.contains(key, Tag.TAG_LIST)) {
                continue;
            }

            ListTag items = handlerData.getList(key, Tag.TAG_COMPOUND);

            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                if (!itemTag.contains("Item", Tag.TAG_COMPOUND)) {
                    continue;
                }

                ItemStack stack = ItemStack.parseOptional(registry, itemTag.getCompound("Item"));
                if (!stack.isEmpty()) {
                    accessories.add(stack.copy());
                }
            }
        }
    }
}
