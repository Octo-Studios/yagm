package it.hurts.sskirillss.yagm.client.particle.type;

import it.hurts.octostudios.octolib.module.particle.trail.ParticleTrailProvider;
import net.minecraft.client.particle.Particle;

public class GraveDustParticleTrail extends ParticleTrailProvider<Particle> {
    public GraveDustParticleTrail(Particle particle) {
        super(particle);
    }

    @Override
    public int getTrailUpdateFrequency() {
        return 1;
    }

    @Override
    public int getTrailMaxLength() {
        return 9;
    }

    @Override
    public int getTrailFadeInColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public int getTrailFadeOutColor() {
        return 0xFFB35A18;
    }

    @Override
    public double getTrailScale() {
        return 0.045;
    }
}
