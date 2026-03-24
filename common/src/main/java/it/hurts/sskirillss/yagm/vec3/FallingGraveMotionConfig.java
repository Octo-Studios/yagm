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

    // --- in-flight physics ---
    Vec3 drag;
    double gravity;

    // --- rotation ---
    float rotSpeedMin;
    float rotSpeedMax;

    // --- launch (applied once at spawn) ---
    /** Horizontal travel distance range in blocks. The required launch speed is derived automatically. */
    float minDistance;
    float maxDistance;
    /** Upward velocity range (blocks/tick). */
    float launchUpMin;
    float launchUpMax;

    // --- lifetime ---
    int maxLifetime;

    public static final FallingGraveMotionConfig DEFAULT = new FallingGraveMotionConfig(
            new Vec3(0.98, 1.0, 0.98),
            -0.04,
            15f, 25f,
            3f, 6f,
            0.55f, 0.65f,
            200
    );

    /** Returns the velocity after applying drag and gravity for one tick. */
    public Vec3 applyPhysics(Vec3 motion) {
        return new Vec3(
                motion.x * drag.x,
                motion.y * drag.y + gravity,
                motion.z * drag.z
        );
    }

    /** Picks a random rotation speed within [{@code rotSpeedMin}, {@code rotSpeedMax}]. */
    public float randomRotSpeed(RandomSource random) {
        return rotSpeedMin + random.nextFloat() * (rotSpeedMax - rotSpeedMin);
    }

    /**
     * Generates a launch velocity that will travel exactly {@code targetDist} horizontal blocks
     * before landing, where {@code targetDist} is sampled uniformly from
     * [{@code minDistance}, {@code maxDistance}].
     *
     * <p>The required horizontal speed is derived analytically from the discrete physics:
     * <pre>
     *   distance = speed × Σ drag^i  (i = 0..landingTick-1)
     *            = speed × (1 − drag^T) / (1 − drag)
     * </pre>
     * Landing tick T is estimated from the upward velocity and gravity using the discrete
     * kinematic sum  y(T) = initialOffset + up·T + gravity·T(T−1)/2 = 0.
     */
    public Vec3 randomLaunchVelocity(RandomSource random) {
        double angle  = random.nextDouble() * Math.PI * 2;
        double up     = launchUpMin + random.nextDouble() * (launchUpMax - launchUpMin);
        double target = minDistance  + random.nextDouble() * (maxDistance  - minDistance);

        double speed = horizontalSpeedForDistance(target, up);

        return new Vec3(Math.cos(angle) * speed, up, Math.sin(angle) * speed);
    }

    /**
     * Calculates the initial horizontal speed (blocks/tick) required to travel exactly
     * {@code distance} blocks given {@code upVelocity}, the configured drag and gravity.
     */
    private double horizontalSpeedForDistance(double distance, double upVelocity) {
        // Discrete landing tick: y(T) = 0.5 + up·T + gravity·T(T−1)/2 = 0
        // → gravity/2·T² + (up − gravity/2)·T + 0.5 = 0  (rearranged)
        double a = gravity / 2.0;
        double b = upVelocity - gravity / 2.0;
        double c = 0.5; // entity spawns 0.5 blocks above the ground
        double discriminant = b * b - 4.0 * a * c;
        int landingTick = (discriminant < 0) ? maxLifetime
                : (int) ((-b - Math.sqrt(discriminant)) / (2.0 * a));
        landingTick = Math.max(1, Math.min(landingTick, maxLifetime));

        // Geometric sum of drag^i over the flight
        double d = drag.x;
        double geomSum = (Math.abs(1.0 - d) < 1e-9)
                ? landingTick
                : (1.0 - Math.pow(d, landingTick)) / (1.0 - d);

        return distance / geomSum;
    }
}
