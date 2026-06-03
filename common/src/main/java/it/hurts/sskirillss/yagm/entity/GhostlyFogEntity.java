package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.client.particle.options.GroundDustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GhostlyFogEntity extends Entity {
    private static final long UNBOUND_GRAVE = Long.MIN_VALUE;
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DENSITY = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_R = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_G = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> COLOR_B = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> EMIT_INTERVAL = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> BOUND_GRAVE = SynchedEntityData.defineId(GhostlyFogEntity.class, EntityDataSerializers.LONG);

    public GhostlyFogEntity(EntityType<? extends GhostlyFogEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(LIFETIME, 90);
        builder.define(RADIUS, 0.9f);
        builder.define(DENSITY, 2);
        builder.define(SCALE, 0.95f);
        builder.define(COLOR_R, 0.80f);
        builder.define(COLOR_G, 0.82f);
        builder.define(COLOR_B, 0.80f);
        builder.define(EMIT_INTERVAL, 2);
        builder.define(BOUND_GRAVE, UNBOUND_GRAVE);
    }

    public void configure(int lifetime, float radius, int density, float scale, float r, float g, float b) {
        configure(lifetime, radius, density, scale, r, g, b, 2);
    }

    public void configure(int lifetime, float radius, int density, float scale, float r, float g, float b, int emitInterval) {
        entityData.set(LIFETIME, Math.max(1, lifetime));
        entityData.set(RADIUS, Math.max(0.1f, radius));
        entityData.set(DENSITY, Mth.clamp(density, 1, 5));
        float sign = scale < 0.0f ? -1.0f : 1.0f;
        float magnitude = Math.max(0.1f, Math.abs(scale));
        entityData.set(SCALE, magnitude * sign);
        entityData.set(COLOR_R, Mth.clamp(r, 0f, 1f));
        entityData.set(COLOR_G, Mth.clamp(g, 0f, 1f));
        entityData.set(COLOR_B, Mth.clamp(b, 0f, 1f));
        entityData.set(EMIT_INTERVAL, Mth.clamp(emitInterval, 1, 20));
    }

    public void bindToGrave(BlockPos pos) {
        entityData.set(BOUND_GRAVE, pos == null ? UNBOUND_GRAVE : pos.asLong());
    }

    public boolean isBoundToGrave(BlockPos pos) {
        return pos != null && entityData.get(BOUND_GRAVE) == pos.asLong();
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);

        if (tickCount >= entityData.get(LIFETIME)) {
            discard();
            return;
        }

        if (!level().isClientSide()) {
            return;
        }

        float radius = entityData.get(RADIUS);
        int density = entityData.get(DENSITY);
        float scale = entityData.get(SCALE);
        float r = entityData.get(COLOR_R);
        float g = entityData.get(COLOR_G);
        float b = entityData.get(COLOR_B);

        int emitInterval = entityData.get(EMIT_INTERVAL);
        if (emitInterval > 1 && (tickCount % emitInterval) != 0) {
            return;
        }

        int spots = Math.max(1, density);
        double baseAngle = tickCount * 0.045 + getId() * 0.7;
        for (int i = 0; i < spots; i++) {
            if (random.nextFloat() < 0.45f) {
                continue;
            }

            double sector = (Math.PI * 2.0) / spots;
            double angle = baseAngle + i * sector + (random.nextDouble() - 0.5) * sector * 0.22;
            double dist = (0.65 + 0.35 * Math.sqrt(random.nextDouble())) * radius;

            double px = getX() + Math.cos(angle) * dist;
            double pz = getZ() + Math.sin(angle) * dist;
            boolean underGround = random.nextFloat() < 0.30f;
            double py = underGround ? getY() - (0.05 + random.nextDouble() * 0.12) : getY() + random.nextDouble() * 0.20;

            double vx = (random.nextDouble() - 0.5) * 0.004;
            double vy = underGround ? (0.0035 + random.nextDouble() * 0.0025) : (0.0015 + random.nextDouble() * 0.0025);
            double vz = (random.nextDouble() - 0.5) * 0.004;

            float localWhiten = random.nextFloat() * 0.16f;
            float pr = Mth.clamp(r + localWhiten * (1f - r), 0f, 1f);
            float pg = Mth.clamp(g + localWhiten * (1f - g), 0f, 1f);
            float pb = Mth.clamp(b + localWhiten * (1f - b), 0f, 1f);

            level().addParticle(new GroundDustParticleOptions(pr, pg, pb, scale), px, py, pz, vx, vy, vz);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        configure(
                tag.getInt("lifetime"),
                tag.getFloat("radius"),
                tag.getInt("density"),
                tag.getFloat("scale"),
                tag.getFloat("r"),
                tag.getFloat("g"),
                tag.getFloat("b"),
                tag.contains("emit_interval") ? tag.getInt("emit_interval") : 2
        );
        if (tag.contains("bound_grave")) {
            entityData.set(BOUND_GRAVE, tag.getLong("bound_grave"));
        } else {
            entityData.set(BOUND_GRAVE, UNBOUND_GRAVE);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("lifetime", entityData.get(LIFETIME));
        tag.putFloat("radius", entityData.get(RADIUS));
        tag.putInt("density", entityData.get(DENSITY));
        tag.putFloat("scale", entityData.get(SCALE));
        tag.putFloat("r", entityData.get(COLOR_R));
        tag.putFloat("g", entityData.get(COLOR_G));
        tag.putFloat("b", entityData.get(COLOR_B));
        tag.putInt("emit_interval", entityData.get(EMIT_INTERVAL));
        tag.putLong("bound_grave", entityData.get(BOUND_GRAVE));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }
}
