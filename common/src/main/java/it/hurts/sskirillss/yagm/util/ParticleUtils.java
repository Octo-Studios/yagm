package it.hurts.sskirillss.yagm.util;

import it.hurts.sskirillss.yagm.client.particle.FireParticle;
import it.hurts.sskirillss.yagm.client.particle.options.FireParticleOptions;
import net.minecraft.core.particles.ParticleOptions;

import java.awt.*;

public class ParticleUtils {
    public static ParticleOptions constructSimpleSpark(Color color, float diameter, int lifetime, float scaleModifier) {
        return new FireParticleOptions(FireParticle.Constructor.builder()
                .color(color.getRGB())
                .diameter(diameter)
                .lifetime(lifetime)
                .scaleModifier(scaleModifier)
                .physical(false)
                .roll(0.5F)
                .build());
    }
}
