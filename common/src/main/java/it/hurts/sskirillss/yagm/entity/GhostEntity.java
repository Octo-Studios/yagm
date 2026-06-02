package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.component.ghost_mode.GhostMood;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.goals.*;
import it.hurts.sskirillss.yagm.init.DamageSourceRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.SoundRegistry;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@Slf4j
public class GhostEntity extends AgeableMob  {
    private static final EntityDataAccessor<String> DATA_MOOD = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_TAME = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_SHY_TIMER = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BEHAVIOR_MODE = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_PITCH = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.FLOAT);

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    @Setter
    @Nullable
    private BlockPos homePos;

    @Nullable
    public BlockPos getHomePos() {
        return homePos;
    }

    public BlockPos getAnchorPos() {
        return homePos != null ? homePos : blockPosition();
    }

    public boolean isTargetNearHome(@Nullable LivingEntity target) {
        if (target == null) return false;
        BlockPos anchor = getAnchorPos();
        double radius = GhostEntityData.CEMETERY_AGGRO_RADIUS * 1.75;
        return target.distanceToSqr(anchor.getX() + 0.5, anchor.getY() + 0.5, anchor.getZ() + 0.5) <= radius * radius;
    }

    private int feedCount = 0;
    private int breedLoveTicks = 0;
    private int tameAttackDelayTicks = 0;
    @Nullable
    private UUID loveCause;
    private boolean spawnParticlesEmitted = false;

    public float xBodyRot = 0f;
    public float xBodyRotO = 0f;

    public GhostEntity(EntityType<? extends AgeableMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new GhostMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return AgeableMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.70)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.01)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        if (!(otherParent instanceof GhostEntity partner)) {
            return null;
        }

        GhostEntity child = EntityRegistry.GHOST.get().create(level);
        if (child == null) {
            return null;
        }

        child.setTame(true);

        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid == null) {
            ownerUuid = partner.getOwnerUUID();
        }
        if (ownerUuid == null) {
            Player loveCause = getLoveCause();
            if (loveCause == null) {
                loveCause = partner.getLoveCause();
            }
            if (loveCause != null) {
                ownerUuid = loveCause.getUUID();
            }
        }

        if (ownerUuid != null) {
            child.setOwnerUUID(ownerUuid);
        }

        child.prepareForOwnerFollow();
        return child;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOOD, GhostMood.DEFAULT.getTextureName());
        builder.define(DATA_TAME, false);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_SHY_TIMER, 0);
        builder.define(DATA_BEHAVIOR_MODE, BehaviorMode.FOLLOW.getSerializedName());
        builder.define(DATA_PITCH, 0.0f);
    }

    @Override
    protected PathNavigation createNavigation(@NotNull Level level) {
        GhostPathNavigator nav = new GhostPathNavigator(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }


    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GhostSnippedAttackGoal(this));
        goalSelector.addGoal(2, new GhostFollowOwnerGoal(this));
        goalSelector.addGoal(3, new GhostWanderGoal(this));
        goalSelector.addGoal(4, new GhostMoodGoal(this));
        goalSelector.addGoal(4, new GhostBreedGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                if (GhostEntity.this.isTame() && GhostEntity.this.getBehaviorMode() == BehaviorMode.FOLLOW) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (GhostEntity.this.isTame() && GhostEntity.this.getBehaviorMode() == BehaviorMode.FOLLOW) {
                    return false;
                }
                return super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !(GhostEntity.this.isTame() && GhostEntity.this.getBehaviorMode() == BehaviorMode.FOLLOW) && super.canUse();
            }
        });

        targetSelector.addGoal(1, new GhostDefendOwnerGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) return false;
                LivingEntity currentTarget = GhostEntity.this.getTarget();
                if (currentTarget != null && currentTarget.isAlive()) return false;
                LivingEntity attacker = GhostEntity.this.getLastHurtByMob();
                if (!isTame()) return isValidUntamedTarget(attacker);
                return false;
            }
        });
        targetSelector.addGoal(3, new GhostAggroGoal(this));
    }

    public boolean isTame() {
        return entityData.get(DATA_TAME);
    }


    @Nullable
    public Player getOwner() {
        return entityData.get(DATA_OWNER_UUID).map(uuid -> level().getPlayerByUUID(uuid)).orElse(null);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        return entityData.get(DATA_OWNER_UUID).map(uuid -> uuid.equals(player.getUUID())).orElse(false);
    }

    public void setTame(boolean tamed) {
        entityData.set(DATA_TAME, tamed);
    }

    public void setOwnerUUID(UUID uuid) {
        entityData.set(DATA_OWNER_UUID, Optional.of(uuid));
    }


    public boolean isInLove() {
        return breedLoveTicks > 0;
    }

    public void setInLove(@Nullable Player player) {
        breedLoveTicks = GhostEntityData.BREED_LOVE_TICKS;
        loveCause = player != null ? player.getUUID() : null;
    }

    public void resetLove() {
        breedLoveTicks = 0;
        loveCause = null;
    }

    @Nullable
    public Player getLoveCause() {
        return loveCause == null ? null : level().getPlayerByUUID(loveCause);
    }

    public boolean canBreedWith(GhostEntity other) {
        if (other == null || other == this) {
            return false;
        }

        if (!isTame() || !other.isTame() || isBaby() || other.isBaby() || getAge() != 0 || other.getAge() != 0 || !isInLove() || !other.isInLove()) {
            return false;
        }

        UUID ownerUuid = getOwnerUUID();
        UUID otherOwnerUuid = other.getOwnerUUID();
        return ownerUuid != null && ownerUuid.equals(otherOwnerUuid);
    }

    public boolean hasTameAttackDelay() {
        return isTame() && tameAttackDelayTicks > 0;
    }

    public String getMoodTextureName() {
        return entityData.get(DATA_MOOD);
    }

    public void setMood(GhostMood mood) {
        entityData.set(DATA_MOOD, mood.getTextureName());
    }

    public BehaviorMode getBehaviorMode() {
        return BehaviorMode.CODEC.byName(entityData.get(DATA_BEHAVIOR_MODE), BehaviorMode.FOLLOW);
    }

    public void setBehaviorMode(BehaviorMode mode) {
        entityData.set(DATA_BEHAVIOR_MODE, mode.getSerializedName());
    }

    public void prepareForOwnerFollow() {
        setTarget(null);
        setBehaviorMode(BehaviorMode.FOLLOW);
        setDeltaMovement(Vec3.ZERO);
        updateMoodByBehavior();
    }

    public int getShyTimer() {
        return entityData.get(DATA_SHY_TIMER);
    }

    public void setShyTimer(int value) {
        entityData.set(DATA_SHY_TIMER, value);
    }

    public void updateMoodByBehavior() {
        switch (getBehaviorMode()) {
            case FOLLOW -> {
                Player owner = getOwner();
                setMood(owner != null && distanceTo(owner) < 20.0 ? GhostMood.HAPPY : GhostMood.SAD);
            }
            case WANDER -> setMood(GhostMood.SAD);
            case STAY -> setMood(GhostMood.NEUTRAL);
        }
    }

    public boolean isValidUntamedTarget(@Nullable LivingEntity target) {
        return target != null && target.isAlive() && !target.getType().is(EntityTypeTags.UNDEAD) && !target.isSpectator() && !(target instanceof Player p && p.isCreative());
    }

    public void spawnHeartParticles(int count) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), count, 0.3, 0.3, 0.3, 0.0);
        }
    }


    public Vec3 getOwnerLookOffset() {
        double time = (tickCount + getId() * 13.0) * 0.08;
        double x = Math.sin(time) * 0.12;
        double y = Math.sin(time * 0.7 + 1.2) * 0.05;
        double z = Math.cos(time * 1.1) * 0.12;
        return new Vec3(x, y, z);
    }


    @Override
    public void tick() {
        this.setNoGravity(true);
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;

        if (level().isClientSide()) {
            xBodyRotO = xBodyRot;
            xBodyRot = entityData.get(DATA_PITCH);
            return;
        }

        tickServerSpawnAndDaylight();
        ensureFollowTeleport();

        if (tameAttackDelayTicks > 0) {
            tameAttackDelayTicks--;
        }

        if (isTame()) {
            if (hasTameAttackDelay() && getTarget() != null) {
                setTarget(null);
            }

            LivingEntity target = getTarget();
            if (target != null) {
                Player owner = getOwner();
                double radius = Math.max(12.0, getAttributeValue(Attributes.FOLLOW_RANGE) * 2.0);
                boolean clearTarget = !target.isAlive() || target.isRemoved() || target instanceof GhostEntity || owner == null;

                if (!clearTarget) {
                    clearTarget = owner.distanceToSqr(target) > radius * radius;
                }

                if (clearTarget) {
                    setTarget(null);
                    if (getShyTimer() <= 0) {
                        updateMoodByBehavior();
                    }
                }
            }

            if (getTarget() == null && getShyTimer() <= 0 && GhostMood.fromString(entityData.get(DATA_MOOD)) == GhostMood.ANGRY) {
                updateMoodByBehavior();
            }
        } else {
            LivingEntity target = getTarget();
            boolean validTarget = isValidUntamedTarget(target) && isTargetNearHome(target);
            if (!validTarget) {
                if (target != null) {
                    setTarget(null);
                }
                if (GhostMood.fromString(entityData.get(DATA_MOOD)) == GhostMood.ANGRY) {
                    setMood(GhostMood.DEFAULT);
                }
            }
        }

        if (breedLoveTicks > 0) {
            breedLoveTicks--;
        }
        separateFromNearbyTamedGhosts();

        if (!isTame() && getTarget() == null) {
            setDeltaMovement(getDeltaMovement().scale(0.85));
        }

        tickServerPitch();
    }

    private void ensureFollowTeleport() {
        if (!isTame() || getBehaviorMode() != BehaviorMode.FOLLOW) {
            return;
        }

        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            return;
        }

        if (distanceTo(owner) <= GhostEntityData.TELEPORT_DISTANCE) {
            return;
        }

        if (getTarget() != null) {
            setTarget(null);
        }

        teleportTo(owner.getX(), owner.getY(), owner.getZ());
    }

    public void setSteeredDeltaMovement(Vec3 desiredVelocity, float maxTurnDegrees, double minTurnSpeedFactor) {
        setDeltaMovement(steerMovementToFacing(desiredVelocity, maxTurnDegrees, minTurnSpeedFactor));
    }

    private Vec3 steerMovementToFacing(Vec3 desiredVelocity, float maxTurnDegrees, double minTurnSpeedFactor) {
        double horizontalSpeed = desiredVelocity.horizontalDistance();
        if (horizontalSpeed <= 1e-6) {
            return desiredVelocity;
        }

        float targetYaw = yawFromHorizontal(desiredVelocity);
        float nextYaw = Mth.rotateIfNecessary(getYRot(), targetYaw, maxTurnDegrees);
        applyVisualYaw(nextYaw);

        float yawDelta = Math.abs(Mth.wrapDegrees(targetYaw - nextYaw));
        double speedFactor = Mth.clamp(1.0D - yawDelta / 120.0D, minTurnSpeedFactor, 1.0D);
        Vec3 forward = horizontalForward(nextYaw).scale(horizontalSpeed * speedFactor);
        return new Vec3(forward.x, desiredVelocity.y, forward.z);
    }

    private void applyVisualYaw(float yaw) {
        setYRot(yaw);
        yHeadRot = yaw;
        yBodyRot = yaw;
    }

    private static float yawFromHorizontal(Vec3 vector) {
        return (float) Math.toDegrees(Mth.atan2(-vector.x, vector.z));
    }

    private static Vec3 horizontalForward(float yawDegrees) {
        float yawRadians = yawDegrees * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yawRadians), 0.0D, Mth.cos(yawRadians));
    }

    private void tickServerPitch() {
        float current = entityData.get(DATA_PITCH);
        float next;

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            Vec3 toTarget = target.getEyePosition().subtract(getEyePosition());
            double h = toTarget.horizontalDistance();
            float raw = (float) Math.toDegrees(Math.atan2(toTarget.y, Math.max(h, 1e-4)));
            float targetPitch = Mth.clamp(-raw, -20f, GhostEntityData.PITCH_MAX_DEGREES);
            next = applyPitchStep(current, targetPitch, 0.3f, 4.0f);
        } else {
            Vec3 mov = getDeltaMovement();
            double h = mov.horizontalDistance();
            boolean orbitFollow = isTame() && getBehaviorMode() == BehaviorMode.FOLLOW;

            if (orbitFollow && h < 0.08 && Math.abs(mov.y) < 0.09) {
                next = applyPitchStep(current, 0f, 0.22f, 2.0f);
            } else if (h > 0.005 || Math.abs(mov.y) > 0.005) {
                double vertical = mov.y;
                if (orbitFollow && h < 0.12 && Math.abs(vertical) < 0.12) {
                    vertical = 0.0;
                }

                float raw = (float) Math.toDegrees(Math.atan2(vertical, Math.max(h, 1e-4)));
                float minPitch = orbitFollow ? -14f : -GhostEntityData.PITCH_MAX_DEGREES;
                float maxPitch = orbitFollow ? 22f : GhostEntityData.PITCH_MAX_DEGREES;
                float targetPitch = Mth.clamp(-raw, minPitch, maxPitch);
                float speed = orbitFollow ? 0.14f : (Math.abs(targetPitch - current) > 5f ? 0.22f : GhostEntityData.PITCH_RETURN_SPEED);
                float maxStep = orbitFollow ? 1.6f : 4.5f;
                next = applyPitchStep(current, targetPitch, speed, maxStep);
            } else {
                float speed = orbitFollow ? 0.12f : GhostEntityData.PITCH_RETURN_SPEED;
                float maxStep = orbitFollow ? 1.2f : 3.5f;
                next = applyPitchStep(current, 0f, speed, maxStep);
            }
        }

        entityData.set(DATA_PITCH, next);
    }

    private static float applyPitchStep(float current, float target, float lerpSpeed, float maxStep) {
        float lerped = Mth.rotLerp(lerpSpeed, current, target);
        float delta = Mth.wrapDegrees(lerped - current);
        return current + Mth.clamp(delta, -maxStep, maxStep);
    }

    private void separateFromNearbyTamedGhosts() {
        if (!isTame()) {
            return;
        }

        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid == null) {
            return;
        }

        Vec3 push = Vec3.ZERO;
        for (GhostEntity other : level().getEntitiesOfClass(GhostEntity.class, getBoundingBox().inflate(1.2D), ghost -> ghost != this && ghost.isTame() && ownerUuid.equals(ghost.getOwnerUUID()))) {
            Vec3 away = position().subtract(other.position());
            double distanceSqr = away.lengthSqr();
            if (distanceSqr < 1e-6D) {
                double angle = Math.toRadians((getId() * 37 + other.getId() * 19) % 360);
                away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(0.01D);
                distanceSqr = away.lengthSqr();
            }

            double distance = Math.sqrt(distanceSqr);
            if (distance < 1.1D) {
                push = push.add(away.scale((1.1D - distance) / (1.1D * distance)));
            }
        }

        if (push.lengthSqr() > 1e-6D) {
            setDeltaMovement(getDeltaMovement().add(push.normalize().scale(0.08D)));
        }
    }


    private void tickServerSpawnAndDaylight() {
        if (!isTame() && level().isDay() && level().canSeeSky(blockPosition())) {
            discard();
            return;
        }

        if (!spawnParticlesEmitted) {
            spawnParticlesEmitted = true;
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() * 0.5, getZ(), 15, 0.4, 0.5, 0.4, 0.02);
            }
        }
    }

    @Override
    public @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        if (level().isClientSide()) {
            return super.mobInteract(player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown() && isTame() && isOwnedBy(player)) {
            BehaviorMode next = getBehaviorMode().next();
            setBehaviorMode(next);
            updateMoodByBehavior();

            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (!isTame()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                if (++feedCount >= GhostEntityData.FEEDS_TO_TAME) {
                    entityData.set(DATA_TAME, true);
                    entityData.set(DATA_OWNER_UUID, Optional.of(player.getUUID()));
                    setTarget(null);
                    feedCount = 0;
                    tameAttackDelayTicks = GhostEntityData.TAME_ATTACK_DELAY_TICKS;
                    prepareForOwnerFollow();
                }

                return InteractionResult.SUCCESS;
            }

            if (!isOwnedBy(player)) {
                return InteractionResult.PASS;
            }

            if (isBaby()) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                ageUp(Math.max(1, GhostEntityData.BABY_GROWTH_PER_FEED / 20), true);
                spawnHeartParticles(isBaby() ? 3 : 8);
                return InteractionResult.SUCCESS;
            }

            if (!(isTame() && !isBaby() && getAge() == 0 && breedLoveTicks <= 0)) {
                return InteractionResult.PASS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            setInLove(player);
            spawnHeartParticles(6);
            return InteractionResult.SUCCESS;
        }

        if (!stack.isEmpty() || !isTame() || !isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        setShyTimer(GhostEntityData.SHY_DURATION_TICKS);
        setMood(GhostMood.SHY);

        spawnHeartParticles(5);

        playSound(random.nextBoolean() ? SoundRegistry.GHOST_PET_1.get() : SoundRegistry.GHOST_PET_2.get(), 1.0f, 0.95f + random.nextFloat() * 0.1f);

        return InteractionResult.SUCCESS;
    }


    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (isTame() && target instanceof Player player && isOwnedBy(player)) {
            return false;
        }

        if (isTame() && target instanceof GhostEntity) {
            return false;
        }

        if (!isTame() && target instanceof LivingEntity livingTarget && livingTarget.getType().is(EntityTypeTags.UNDEAD)) {
            return false;
        }

        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = DamageSourceRegistry.of(level(), DamageSourceRegistry.GHOST, this);

        boolean result = target.hurt(source, damage);
        if (!result || !(target instanceof LivingEntity)) {
            return result;
        }

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !level().isClientSide()) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

            if (isTame() && attacker instanceof Player player && isOwnedBy(player)) {
                if (getTarget() == null) {
                    updateMoodByBehavior();
                }
                return true;
            }

            setMood(GhostMood.ANGRY);

            if (!isTame() && isValidUntamedTarget(attacker)) {
                setTarget(attacker);
            }
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        var deathMessage = this.getCombatTracker().getDeathMessage();
        super.die(source);
        if (this.dead && !level().isClientSide() && level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && getOwner() instanceof ServerPlayer owner) {
            owner.sendSystemMessage(deathMessage);
        }
    }


    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        int i = random.nextInt(3);

        if (i == 0) {
            return SoundRegistry.GHOST_IDLE_1.get();
        }

        if (i == 1) {
            return SoundRegistry.GHOST_IDLE_2.get();
        }

        return SoundRegistry.GHOST_IDLE_3.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return random.nextBoolean() ? SoundRegistry.GHOST_HURT_1.get() : SoundRegistry.GHOST_HURT_2.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundRegistry.GHOST_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return isTame() || GhostMood.fromString(entityData.get(DATA_MOOD)) == GhostMood.ANGRY;
    }

    @Override
    public void push(@NotNull Entity entity) {
        if (isTame() && entity instanceof GhostEntity other && other.isTame()) {
            UUID ownerUuid = getOwnerUUID();
            if (ownerUuid != null && ownerUuid.equals(other.getOwnerUUID())) {
                return;
            }
        }

        super.push(entity);
    }

    @Override
    protected void pushEntities() {
        if (isTame()) {
            super.pushEntities();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString(KEYS.getMood(), entityData.get(DATA_MOOD));
        tag.putBoolean(KEYS.getTame(), entityData.get(DATA_TAME));
        tag.putString(KEYS.getBehaviorMode(), entityData.get(DATA_BEHAVIOR_MODE));
        tag.putInt(KEYS.getFeedCount(), feedCount);
        tag.putInt(KEYS.getGhostAge(), getAge());
        tag.putInt(KEYS.getBreedLoveTicks(), breedLoveTicks);
        entityData.get(DATA_OWNER_UUID).ifPresent(uuid -> tag.putUUID(KEYS.getOwnerUuid(), uuid));

        if (homePos != null) {
            tag.putLong(KEYS.getHomePos(), homePos.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_MOOD, GhostMood.fromString(tag.getString(KEYS.getMood())).getTextureName());
        entityData.set(DATA_TAME, tag.getBoolean(KEYS.getTame()));
        feedCount = tag.getInt(KEYS.getFeedCount());
        if (tag.contains(KEYS.getGhostAge())) {
            setAge(tag.getInt(KEYS.getGhostAge()));
        }
        breedLoveTicks = tag.getInt(KEYS.getBreedLoveTicks());
        loveCause = null;
        setBehaviorMode(BehaviorMode.CODEC.byName(tag.getString(KEYS.getBehaviorMode()), BehaviorMode.FOLLOW));

        if (tag.hasUUID(KEYS.getOwnerUuid())) {
            entityData.set(DATA_OWNER_UUID, Optional.of(tag.getUUID(KEYS.getOwnerUuid())));
        }

        if (tag.contains(KEYS.getHomePos())) {
            homePos = BlockPos.of(tag.getLong(KEYS.getHomePos()));
        }
    }

    @Override
    public boolean shouldDropExperience() {
        return true;
    }

    @Override
    protected boolean isAlwaysExperienceDropper() {
        return true;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 5;
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
    }
}
