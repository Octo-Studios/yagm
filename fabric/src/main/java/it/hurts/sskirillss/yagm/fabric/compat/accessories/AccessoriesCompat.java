package it.hurts.sskirillss.yagm.fabric.compat.accessories;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import it.hurts.sskirillss.yagm.api.compat.accessories.AccessoryContainerView;
import it.hurts.sskirillss.yagm.api.compat.accessories.BaseAccessoriesCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AccessoriesCompat extends BaseAccessoriesCompat {

    @Override
    public boolean isModLoaded() {
        return FabricLoader.getInstance().isModLoaded("accessories");
    }

    @Override
    protected Optional<Map<String, AccessoryContainerView>> getContainers(ServerPlayer player) {
        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
            return Optional.empty();
        }

        Map<String, AccessoryContainerView> views = new HashMap<>();
        for (Map.Entry<String, AccessoriesContainer> entry : capabilityOpt.get().getContainers().entrySet()) {
            views.put(entry.getKey(), new FabricAccessoryContainerView(entry.getValue()));
        }
        return Optional.of(views);
    }

    @Override
    protected boolean canEquipAccessory(ServerPlayer player, ItemStack stack) {
        return !AccessoriesAPI.getStackSlotTypes(player, stack).isEmpty();
    }

    @Override
    protected boolean tryEquipAccessoryInternal(ServerPlayer player, ItemStack stack) {
        Optional<AccessoriesCapability> capabilityOpt = AccessoriesCapability.getOptionally(player);
        if (capabilityOpt.isEmpty()) {
            return false;
        }

        AccessoriesCapability capability = capabilityOpt.get();
        var slotReference = capability.attemptToEquipAccessory(stack.copy());
        return slotReference != null && slotReference.isValid();
    }

    private static final class FabricAccessoryContainerView implements AccessoryContainerView {
        private final AccessoriesContainer container;
        private final ExpandedSimpleContainer main;
        private final ExpandedSimpleContainer cosmetic;

        private FabricAccessoryContainerView(AccessoriesContainer container) {
            this.container = container;
            this.main = container.getAccessories();
            this.cosmetic = container.getCosmeticAccessories();
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
}
