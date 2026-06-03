package it.hurts.sskirillss.yagm.api.compat.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import it.hurts.sskirillss.yagm.api.compat.BaseAccessoryCompat;
import net.minecraft.core.BlockPos;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


public abstract class AccessoriesCompatImpl extends BaseAccessoryCompat {

    @Override
    protected String getNbtTag() {
        return "AccessoriesData";
    }

    @Override
    public String getModName() {
        return "Accessories";
    }

    @Override
    public Map<String, ItemStack> collectAccessories(ServerPlayer player) {
        Map<String, ItemStack> accessories = new HashMap<>();

        Optional<Map<String, AccessoryContainer>> containersOpt = getContainers(player);
        if (containersOpt.isEmpty()) return accessories;

        for (AccessoryContainer container : containersOpt.get().values()) {
            String slotName = container.slotName();
            int size = container.size();

            for (int i = 0; i < size; i++) {
                ItemStack stack = container.getMain(i);
                if (!stack.isEmpty()) {
                    accessories.put(slotName + "/" + i, stack.copy());
                }
            }

            if (container.hasCosmetic()) {
                for (int i = 0; i < size; i++) {
                    ItemStack stack = container.getCosmetic(i);
                    if (!stack.isEmpty()) {
                        accessories.put(slotName + "/cosmetic/" + i, stack.copy());
                    }
                }
            }
        }

        return accessories;
    }


    @Override
    public void clearAccessories(ServerPlayer player) {
        Optional<Map<String, AccessoryContainer>> containersOpt = getContainers(player);
        if (containersOpt.isEmpty()) return;

        for (AccessoryContainer container : containersOpt.get().values()) {
            int size = container.size();

            for (int i = 0; i < size; i++) {
                container.setMain(i, ItemStack.EMPTY);
            }

            if (container.hasCosmetic()) {
                for (int i = 0; i < size; i++) {
                    container.setCosmetic(i, ItemStack.EMPTY);
                }
            }

            container.markChanged();
        }
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull) {
        restoreAccessories(player, accessories, dropIfFull, null, null);
    }

    @Override
    public void restoreAccessories(ServerPlayer player, Map<String, ItemStack> accessories, boolean dropIfFull, Level level, BlockPos dropPos) {
        if (accessories.isEmpty()) {
            return;
        }

        Optional<Map<String, AccessoryContainer>> containersOpt = getContainers(player);

        if (containersOpt.isEmpty()) {
            if (level != null && dropPos != null) {
                getInventoryOrDrop(player, accessories.values(), dropIfFull, level, dropPos);
            } else {
                getInventoryOrDrop(player, accessories.values(), dropIfFull);
            }
            return;
        }

        Map<String, AccessoryContainer> containers = containersOpt.get();

        for (Map.Entry<String, ItemStack> entry : accessories.entrySet()) {
            ItemStack stack = entry.getValue();

            if (stack.isEmpty()) {
                continue;
            }

            SlotInfo slotInfo = parseSlotKey(entry.getKey());
            boolean restored = false;

            if (slotInfo != null) {
                AccessoryContainer container = containers.get(slotInfo.getSlotName());

                if (container != null) {
                    if (slotInfo.getIndex() >= 0 && slotInfo.getIndex() < container.size()) {
                        ItemStack existing;

                        if (slotInfo.isCosmetic()) {
                            existing = container.getCosmetic(slotInfo.getIndex());
                        } else {
                            existing = container.getMain(slotInfo.getIndex());
                        }

                        boolean hasTarget;

                        if (slotInfo.isCosmetic()) {
                            hasTarget = container.hasCosmetic();
                        } else {
                            hasTarget = true;
                        }

                        if (hasTarget && existing.isEmpty()) {
                            if (slotInfo.isCosmetic()) {
                                container.setCosmetic(slotInfo.getIndex(), stack.copy());
                            } else {
                                container.setMain(slotInfo.getIndex(), stack.copy());
                            }

                            container.markChanged();
                            restored = true;
                        }
                    }
                }
            }

            if (!restored) {
                if (!tryEquipAccessory(player, stack)) {
                    if (level != null && dropPos != null) {
                        getInventoryOrDrop(player, stack, dropIfFull, level, dropPos);
                    } else {
                        getInventoryOrDrop(player, stack, dropIfFull);
                    }
                }
            }
        }
    }

    @Override
    public boolean canEquipAsAccessory(ServerPlayer player, ItemStack stack) {
        return !stack.isEmpty() && !AccessoriesAPI.getStackSlotTypes(player, stack).isEmpty();
    }


    @Override
    public boolean tryEquipAccessory(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;

        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) return false;

        var slotReference = capabilityOpt.get().attemptToEquipAccessory(stack.copy());
        return slotReference != null && slotReference.isValid();
    }


    private Optional<Map<String, AccessoryContainer>> getContainers(ServerPlayer player) {
        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) return Optional.empty();

        Map<String, AccessoryContainer> views = new HashMap<>();
        for (Map.Entry<String, AccessoriesContainer> entry : capabilityOpt.get().getContainers().entrySet()) {
            views.put(entry.getKey(), AccessoriesContainerAdapter.of(entry.getValue()));
        }
        return Optional.of(views);
    }


    private SlotInfo parseSlotKey(String key) {
        if (key == null || key.isEmpty()) return null;

        String[] parts = key.split("/");

        if (parts.length == 2) {
            // "slotName/index"
            try {
                return new SlotInfo(parts[0], Integer.parseInt(parts[1]), false);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (parts.length == 3 && "cosmetic".equals(parts[1])) {
            // "slotName/cosmetic/index"
            try {
                return new SlotInfo(parts[0], Integer.parseInt(parts[2]), true);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class AccessoriesContainerAdapter implements AccessoryContainer {

        private final AccessoriesContainer container;
        private final ExpandedSimpleContainer main;
        private final ExpandedSimpleContainer cosmetic;

        static AccessoriesContainerAdapter of(AccessoriesContainer container) {
            return new AccessoriesContainerAdapter(container, container.getAccessories(), container.getCosmeticAccessories());
        }

        @Override
        public String slotName() {
            return container.getSlotName();
        }

        @Override
        public int size() {
            return container.getSize();
        }

        @Override
        public ItemStack getMain(int index) {
            return main.getItem(index);
        }

        @Override
        public void setMain(int index, ItemStack stack) {
            main.setItem(index, stack);
        }

        @Override
        public boolean hasCosmetic() {
            return cosmetic != null;
        }

        @Override
        public ItemStack getCosmetic(int index) {
            return cosmetic == null ? ItemStack.EMPTY : cosmetic.getItem(index);
        }

        @Override
        public void setCosmetic(int index, ItemStack stack) {
            if (cosmetic != null) {
                cosmetic.setItem(index, stack);
            }
        }

        @Override
        public void markChanged() {
            container.markChanged();
        }
    }


    @Value
    private static class SlotInfo {
        String slotName;
        int index;
        boolean cosmetic;
    }
}
