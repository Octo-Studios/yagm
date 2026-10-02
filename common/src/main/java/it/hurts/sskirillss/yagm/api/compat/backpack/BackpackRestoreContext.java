package it.hurts.sskirillss.yagm.api.compat.backpack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record BackpackRestoreContext(boolean dropIfFull, @Nullable Level level, @Nullable BlockPos dropPos) {

    public boolean hasDropPosition() {
        return level != null && dropPos != null;
    }
}
