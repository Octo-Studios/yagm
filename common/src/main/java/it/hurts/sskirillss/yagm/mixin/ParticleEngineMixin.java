package it.hurts.sskirillss.yagm.mixin;

import com.google.common.collect.ImmutableList;
import it.hurts.sskirillss.yagm.client.particle.FireParticle;
import it.hurts.sskirillss.yagm.client.particle.GhostlyFogParticle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Shadow
    @Final
    @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyRenderOrder(CallbackInfo ci) {
        var order = new ArrayList<>(RENDER_ORDER);

        int fireIndex = Math.min(4, order.size());
        order.add(fireIndex, FireParticle.RENDERER_TRANSLUCENT);

        int fogIndex = Math.min(fireIndex + 1, order.size());
        order.add(fogIndex, GhostlyFogParticle.RENDERER);

        RENDER_ORDER = ImmutableList.copyOf(order);
    }
}