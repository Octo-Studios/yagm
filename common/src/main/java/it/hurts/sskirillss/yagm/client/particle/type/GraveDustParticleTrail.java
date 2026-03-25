package it.hurts.sskirillss.yagm.client.particle.type;

import it.hurts.octostudios.octolib.module.particle.trail.ParticleTrailProvider;
import it.hurts.sskirillss.yagm.client.particle.GraveTrailParticle;

public class GraveDustParticleTrail extends ParticleTrailProvider<GraveTrailParticle> {
    public GraveDustParticleTrail(GraveTrailParticle particle) {
        super(particle);
    }

    @Override
    public int getTrailUpdateFrequency() {
        return 1;
    }

    @Override
    public int getTrailMaxLength() {
        return 16;
    }

    @Override
    public int getTrailFadeInColor() {
        return particle.getTrailColorIn();
    }

    @Override
    public int getTrailFadeOutColor() {
        return particle.getTrailColorOut();
    }

    @Override
    public double getTrailScale() {
        return 0.02;
    }
}
