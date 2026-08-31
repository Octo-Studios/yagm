package it.hurts.sskirillss.yagm.vec3;

import lombok.Value;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

@Value
public class FallingGraveMotionConfig {

    Vec3 drag;
    double gravity;
    float rotSpeedMin;
    float rotSpeedMax;
    float minDistance;
    float maxDistance;
    float launchUpMin;
    float launchUpMax;
    int maxLifetime;

    public static final FallingGraveMotionConfig DEFAULT = new FallingGraveMotionConfig(new Vec3(0.98, 1.0, 0.98), -0.04, 15f,25f, 3f, 6f, 0.55f, 0.65f, 200);

    public Vec3 applyPhysics(Vec3 motion) {
        return new Vec3(motion.x * drag.x, motion.y * drag.y + gravity, motion.z * drag.z);
    }

    public float randomRotSpeed(RandomSource random) {
        return rotSpeedMin + random.nextFloat() * (rotSpeedMax - rotSpeedMin);
    }

    public Vec3 randomLaunchVelocity(RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double up = launchUpMin + random.nextDouble() * (launchUpMax - launchUpMin);
        double target = minDistance  + random.nextDouble() * (maxDistance  - minDistance);

        double speed = horizontalSpeedForDistance(target, up);

        return new Vec3(Math.cos(angle) * speed, up, Math.sin(angle) * speed);
    }


    private double horizontalSpeedForDistance(double distance, double upVelocity) {
        // y(T) = 0.5 + up·T + gravity·T(T−1)/2 = 0
        // → gravity/2·T² + (up − gravity/2)·T + 0.5 = 0
        double g_func = gravity / 2.0;
        double velg = upVelocity - gravity / 2.0;
        double h_val_fixed = 0.5;
        double discriminant = velg * velg - 4.0 * g_func * h_val_fixed;
        int landingTick = (discriminant < 0) ? maxLifetime : (int) ((-velg - Math.sqrt(discriminant)) / (2.0 * g_func));
        landingTick = Math.clamp(landingTick, 1, maxLifetime);

        double d = drag.x;
        double geomSum = (Math.abs(1.0 - d) < 1e-9) ? landingTick : (1.0 - Math.pow(d, landingTick)) / (1.0 - d);

        return distance / geomSum;
    }
}
