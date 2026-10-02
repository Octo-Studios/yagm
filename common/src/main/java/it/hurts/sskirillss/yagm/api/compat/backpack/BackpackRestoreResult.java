package it.hurts.sskirillss.yagm.api.compat.backpack;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record BackpackRestoreResult(boolean restored, List<ItemStack> remainder, String reason) {

    public BackpackRestoreResult {
        if (remainder == null) {
            remainder = List.of();
        } else {
            List<ItemStack> copied = new ArrayList<>();

            for (ItemStack stack : remainder) {
                Objects.requireNonNull(stack, "remainder stack");

                if (!stack.isEmpty()) {
                    copied.add(stack.copy());
                }
            }

            remainder = List.copyOf(copied);
        }

        reason = Objects.requireNonNullElse(reason, "");
    }

    public static BackpackRestoreResult success() {
        return new BackpackRestoreResult(true, List.of(), "restored");
    }

    public static BackpackRestoreResult pass(ItemStack stack, String reason) {
        return new BackpackRestoreResult(false, stack.isEmpty() ? List.of() : List.of(stack.copy()), reason);
    }
}
