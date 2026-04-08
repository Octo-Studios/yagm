package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.component.ghost_mode.GhostMood;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.entity.goals.*;
import it.hurts.sskirillss.yagm.init.DamageSourceRegistry;
import it.hurts.sskirillss.yagm.init.SoundRegistry;
import it.hurts.sskirillss.yagm.util.NbtKeys;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
public class GhostEntity extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_MOOD = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_TAME = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> DATA_SHY_TIMER = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_BEHAVIOR_MODE = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.STRING);

    private static final NbtKeys KEYS = NbtKeys.INSTANCE;

    @Setter
    @Nullable
    private BlockPos homePos;

    private int feedCount = 0;
    private boolean spawnParticlesEmitted = false;

    public float xBodyRot = 0f;
    public float xBodyRotO = 0f;

    public GhostEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new GhostMoveControl(this);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder setCustomAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.70)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.01)
                .add(Attributes.ATTACK_KNOCKBACK, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.FLYING_SPEED, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOOD, GhostMood.DEFAULT.getTextureName());
        builder.define(DATA_TAME, false);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_SHY_TIMER, 0);
        builder.define(DATA_BEHAVIOR_MODE, BehaviorMode.FOLLOW.getSerializedName());
    }

    @Override
    protected PathNavigation createNavigation(@NotNull Level level) {
        GhostPathNavigator nav = new GhostPathNavigator(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public String getMoodTextureName() {
        return entityData.get(DATA_MOOD);
    }

    public void setMood(GhostMood mood) {
        entityData.set(DATA_MOOD, mood.getTextureName());
    }

    public boolean isTame() {
        return entityData.get(DATA_TAME);
    }

    public BehaviorMode getBehaviorMode() {
        return BehaviorMode.CODEC.byName(entityData.get(DATA_BEHAVIOR_MODE), BehaviorMode.FOLLOW);
    }

    public void setBehaviorMode(BehaviorMode mode) {
        entityData.set(DATA_BEHAVIOR_MODE, mode.getSerializedName());
    }

    @Nullable
    public Player getOwner() {
        UUID uuid = entityData.get(DATA_OWNER_UUID).orElse(null);
        if (uuid == null) {
            return null;
        }

        return level().getPlayerByUUID(uuid);
    }

    public boolean isOwnedBy(Player player) {
        return entityData.get(DATA_OWNER_UUID).map(uuid -> uuid.equals(player.getUUID())).orElse(false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new GhostSnippedAttackGoal(this));
        goalSelector.addGoal(2, new GhostFollowOwnerGoal(this));
        goalSelector.addGoal(3, new GhostWanderGoal(this));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new GhostDefendOwnerGoal(this));
        targetSelector.addGoal(2, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (!super.canUse()) {
                    return false;
                }

                if (!GhostEntity.this.isTame()) {
                    return true;
                }

                LivingEntity attacker = GhostEntity.this.getLastHurtByMob();
                return !(attacker instanceof Player p && GhostEntity.this.isOwnedBy(p));
            }
        });
        targetSelector.addGoal(3, new GhostAggroGoal(this));
    }

    @Override
    public void tick() {
        this.setNoGravity(true);
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;

        if (level().isClientSide()) {
            xBodyRotO = xBodyRot;

            Vec3 lookVector = null;
            boolean forceOwnerLook = false;

            LivingEntity target = getTarget();
            if (target != null && target.isAlive()) {
                lookVector = target.getEyePosition().subtract(getEyePosition());
            } else if (isTame() && getBehaviorMode() == BehaviorMode.FOLLOW && GhostMood.fromString(entityData.get(DATA_MOOD)) != GhostMood.ANGRY) {
                Player owner = getOwner();
                if (owner != null && !owner.isSpectator()) {
                    Vec3 ownerFocusPos = owner.getEyePosition().add(getOwnerLookOffset());
                    lookVector = ownerFocusPos.subtract(getEyePosition());
                    forceOwnerLook = true;

                    double ownerHorizontal = lookVector.horizontalDistance();
                    if (ownerHorizontal > 1e-4) {
                        float lookYaw = (float) Math.toDegrees(Math.atan2(lookVector.z, lookVector.x)) - 90f;
                        setYRot(lookYaw);
                        yHeadRot = yBodyRot = lookYaw;
                        yHeadRotO = yBodyRotO = lookYaw;
                    }
                }
            }

            if (forceOwnerLook && lookVector != null && lookVector.lengthSqr() > 1e-6) {
                double horizontalForPitch = Math.max(lookVector.horizontalDistance(), 1e-4);
                float targetPitch = (float) Mth.clamp(-Math.toDegrees(Math.atan2(lookVector.y, horizontalForPitch)), -GhostEntityData.PITCH_MAX_DEGREES, GhostEntityData.PITCH_MAX_DEGREES);
                xBodyRot = targetPitch;
                return;
            }

            if (lookVector == null || lookVector.lengthSqr() < 1e-6) {
                lookVector = getDeltaMovement();
            }

            double horizontalDistance = lookVector.horizontalDistance();
            if (lookVector.lengthSqr() > 1e-6 && (horizontalDistance > 1e-4 || Math.abs(lookVector.y) > 0.002)) {
                double horizontalForPitch = Math.max(horizontalDistance, 1e-4);
                float rawAngle = (float) Math.toDegrees(Math.atan2(lookVector.y, horizontalForPitch));
                float targetPitch = (float) Mth.clamp(-rawAngle, -GhostEntityData.PITCH_MAX_DEGREES, GhostEntityData.PITCH_MAX_DEGREES);
                float lerpSpeed = Math.abs(targetPitch - xBodyRot) > 8f ? 0.25f : GhostEntityData.PITCH_RETURN_SPEED;
                xBodyRot = Mth.rotLerp(lerpSpeed, xBodyRot, targetPitch);
                return;
            }

            xBodyRot = Mth.rotLerp(GhostEntityData.PITCH_RETURN_SPEED, xBodyRot, 0f);
            return;
        }

        if (tickServerSpawnAndDaylight()) {
            return;
        }

        int shy = entityData.get(DATA_SHY_TIMER);
        if (shy > 0) {
            int next = shy - 1;
            entityData.set(DATA_SHY_TIMER, next);

            if (next == 0 && isTame()) {
                updateMoodByBehavior();
            }
        }

        if (tickCount % GhostEntityData.MOOD_UPDATE_INTERVAL == 0) {
            LivingEntity target = getTarget();

            if (isTame()) {
                if (entityData.get(DATA_SHY_TIMER) <= 0) {
                    if (target != null && target.isAlive()) {
                        setMood(GhostMood.ANGRY);
                    } else {
                        updateMoodByBehavior();
                    }
                }
                return;
            }

            boolean validTarget = target != null && target.isAlive() && !target.isSpectator() && !(target instanceof Player p && p.isCreative());

            if (!validTarget && target != null) {
                setTarget(null);
            }

            setMood(validTarget ? GhostMood.ANGRY : GhostMood.DEFAULT);
        }

        if (!isTame() && getTarget() == null) {
            setDeltaMovement(getDeltaMovement().scale(0.85));
        }
    }

    private Vec3 getOwnerLookOffset() {
        double time = (tickCount + getId() * 13.0) * 0.08;
        double x = Math.sin(time) * 0.12;
        double y = Math.sin(time * 0.7 + 1.2) * 0.05;
        double z = Math.cos(time * 1.1) * 0.12;
        return new Vec3(x, y, z);
    }

    private boolean tickServerSpawnAndDaylight() {
        if (!spawnParticlesEmitted) {
            spawnParticlesEmitted = true;
            spawnSoulParticleBurst();
        }

        if (!isTame() && level().isDay()) {
            spawnSoulParticleBurst();
            discard();
            return true;
        }

        return false;
    }

    private void updateMoodByBehavior() {
        switch (getBehaviorMode()) {
            case FOLLOW -> {
                Player owner = getOwner();
                setMood(owner != null && distanceTo(owner) < 20.0 ? GhostMood.HAPPY : GhostMood.SAD);
            }
            case WANDER -> setMood(GhostMood.SAD);
            case STAY -> setMood(GhostMood.NEUTRAL);
        }
    }

    @Override
    protected @NotNull InteractionResult mobInteract(Player player, @NotNull InteractionHand hand) {
        if (level().isClientSide()) {
            return super.mobInteract(player, hand);
        }

        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && isTame() && isOwnedBy(player)) {
            BehaviorMode next = getBehaviorMode().next();
            setBehaviorMode(next);
            updateMoodByBehavior();

            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(next.getTranslationKey()), true);
            }

            return InteractionResult.SUCCESS;
        }

        if (stack.is(Items.GLOWSTONE_DUST)) {
            if (isTame()) {
                return InteractionResult.PASS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (++feedCount >= GhostEntityData.FEEDS_TO_TAME) {
                entityData.set(DATA_TAME, true);
                entityData.set(DATA_OWNER_UUID, Optional.of(player.getUUID()));
                setTarget(null);
                feedCount = 0;
                setBehaviorMode(BehaviorMode.FOLLOW);
                setMood(GhostMood.HAPPY);

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("yagm.ghost.tamed"), true);
                }
            }

            return InteractionResult.SUCCESS;
        }

        if (!stack.isEmpty() || !isTame() || !isOwnedBy(player)) {
            return super.mobInteract(player, hand);
        }

        entityData.set(DATA_SHY_TIMER, GhostEntityData.SHY_DURATION_TICKS);
        setMood(GhostMood.SHY);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }

        playSound(random.nextBoolean() ? SoundRegistry.GHOST_PET_1.get() : SoundRegistry.GHOST_PET_2.get(), 1.0f, 0.95f + random.nextFloat() * 0.1f);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable("yagm.ghost.petted"), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        if (isTame() && target instanceof Player player && isOwnedBy(player)) {
            return false;
        }

        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        DamageSource source = DamageSourceRegistry.of(level(), DamageSourceRegistry.GHOST, this);

        boolean result = target.hurt(source, damage);
        if (!result || !(target instanceof LivingEntity living)) {
            return result;
        }

        float knockback = (float) getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        if (knockback > 0) {
            Vec3 push = getDeltaMovement().multiply(1, 0, 1).normalize().scale(knockback * 0.5);
            living.push(push.x, 0.1, push.z);
        }

        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);

        if (result && !level().isClientSide()) {
            setMood(GhostMood.ANGRY);
            if (isTame() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player p && isOwnedBy(p))) {
                setTarget(attacker);
            }
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide() && isTame() && level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES)) {
            Player owner = getOwner();
            if (owner instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(getCombatTracker().getDeathMessage());
            }
        }

        super.die(source);
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

    private void spawnSoulParticleBurst() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() * 0.5, getZ(), 15, 0.4, 0.5, 0.4, 0.02);
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !isTame();
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
    protected int getBaseExperienceReward() {
        return 5;
    }
}