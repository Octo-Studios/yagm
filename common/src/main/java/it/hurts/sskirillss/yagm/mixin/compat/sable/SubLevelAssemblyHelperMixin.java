package it.hurts.sskirillss.yagm.mixin.compat.sable;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import it.hurts.sskirillss.yagm.api.compat.sable.SableCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private static void updateGravePositions(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        SableCompat.onBlocksMoved(level, transform, blocks);
    }
}
