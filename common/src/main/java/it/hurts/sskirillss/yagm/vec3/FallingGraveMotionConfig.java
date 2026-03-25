package it.hurts.sskirillss.yagm.vec3;

import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import lombok.Value;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Immutable configuration for {@link FallingGraveEntity} physics and rotation.
 *
 * <ul>
 *   <li>{@code drag}     – per-axis velocity multiplier applied every tick (x/z typically < 1.0 for air resistance,
 *                          y = 1.0 because gravity is handled separately via {@code gravity}).</li>
 *   <li>{@code gravity}  – constant added to {@code velocity.y} every tick; negative = falling.</li>
 *   <li>{@code rotSpeedMin/Max} – random rotation speed range in degrees per tick.</li>
 *   <li>{@code maxLifetime} – ticks before the grave is forced to place itself.</li>
 * </ul>
 */
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

    public static final FallingGraveMotionConfig DEFAULT = new FallingGraveMotionConfig(
            new Vec3(0.98, 1.0, 0.98),
            -0.04,
            15f, 25f,
            3f, 6f,
            0.55f, 0.65f,
            200
    );


    public Vec3 applyPhysics(Vec3 motion) {
        return new Vec3(
                motion.x * drag.x,
                motion.y * drag.y + gravity,
                motion.z * drag.z
        );
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
        // → gravity/2·T² + (up − gravity/2)·T + 0.5 = 0  (rearranged)
        double a = gravity / 2.0;
        double b = upVelocity - gravity / 2.0;
        double c = 0.5;
        double discriminant = b * b - 4.0 * a * c;
        int landingTick = (discriminant < 0) ? maxLifetime : (int) ((-b - Math.sqrt(discriminant)) / (2.0 * a));
        landingTick = Math.max(1, Math.min(landingTick, maxLifetime));

        double d = drag.x;
        double geomSum = (Math.abs(1.0 - d) < 1e-9) ? landingTick : (1.0 - Math.pow(d, landingTick)) / (1.0 - d);

        return distance / geomSum;
    }
}
