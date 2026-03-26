package it.hurts.sskirillss.yagm.entity;

import it.hurts.sskirillss.yagm.component.ghost_mode.BehaviorMode;
import it.hurts.sskirillss.yagm.component.ghost_mode.GhostMood;
import it.hurts.sskirillss.yagm.data.entitydata.GhostEntityData;
import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import java.util.*;



@Slf4j
public class GhostEntity extends PathfinderMob {

    private static final EntityDataAccessor<String> DATA_MOOD = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_TAME = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(GhostEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    @Getter
    private BehaviorMode behaviorMode = BehaviorMode.FOLLOW;
    private int shyTimer = 0;
    private int feedCount = 0;
    @Nullable
    private BlockPos targetGravePos;
    @Setter
    @Nullable
    private BlockPos homePos;
    private Vec3 smoothVelocity = Vec3.ZERO;
    private int stealCooldown = 0;
    private final Set<BlockPos> visitedGraves = new HashSet<>();
    private int graveListRefreshTimer = 0;
    private List<BlockPos> cachedGraves = new ArrayList<>();
    private final NonNullList<ItemStack> stolenItems = NonNullList.create();
    private boolean spawnParticlesEmitted = false;


    public GhostEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new GhostMoveControl(this);
        this.setNoGravity(true);
    }


    public static AttributeSupplier.Builder setCustomAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ATTACK_KNOCKBACK, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.FLYING_SPEED, 0.3);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_MOOD, GhostMood.DEFAULT.getTextureName());
        builder.define(DATA_TAME, false);
        builder.define(DATA_OWNER_UUID, Optional.empty());
    }


    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }


    public GhostMood getMood() {
        return GhostMood.fromString(this.entityData.get(DATA_MOOD));
    }

    public void setMood(GhostMood mood) {
        this.entityData.set(DATA_MOOD, mood.getTextureName());
    }

    public String getMoodTextureName() {
        return this.entityData.get(DATA_MOOD);
    }


    public boolean isTame() {
        return this.entityData.get(DATA_TAME);
    }

    public void setTame(boolean tame) {
        this.entityData.set(DATA_TAME, tame);
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    @Nullable
    public Player getOwner() {
        UUID uuid = getOwnerUUID();
        if (uuid == null) return null;
        return level().getPlayerByUUID(uuid);
    }

    public void tame(Player player) {
        setTame(true);
        setOwnerUUID(player.getUUID());
        targetGravePos = null;
        visitedGraves.clear();
        cachedGraves.clear();
    }


    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                if (GhostEntity.this.isTame() && getTarget() instanceof Player p && GhostEntity.this.isOwnedBy(p)) {
                    return false;
                }
                return super.canUse();
            }

            @Override
            public void start() {
                super.start();
                LivingEntity target = mob.getTarget();
                if (target instanceof Player p && GhostEntity.this.isTame() && GhostEntity.this.isOwnedBy(p)) {
                    mob.setTarget(null);
                }
            }
        });
        this.targetSelector.addGoal(2, new GhostAggroGoal(this));
    }


    @Override
    public void tick() {
        super.tick();

        if (shyTimer > 0) {
            shyTimer--;
            if (shyTimer == 0 && isTame()) updateMoodFromState();
        }

        if (!level().isClientSide()) {
            serverTick();
        } else {
            clientTick();
        }
    }

    private void serverTick() {
        if (!spawnParticlesEmitted) {
            spawnParticlesEmitted = true;
            spawnSoulParticleBurst();
        }

        if (!isTame() && level().isDay()) {
            spawnSoulParticleBurst();
            discard();
            return;
        }

        if (stealCooldown > 0) {
            stealCooldown--;
        }

        if (tickCount % 10 == 0) {
            if (isTame()) {
                updateTamedMood();
            } else {
                updateUntamedMood();
            }
        }

        if (getMood() == GhostMood.ANGRY && getTarget() != null && getTarget().isAlive()) {
            tickAngryChase();
        } else if (isTame()) {
            tickTamedBehavior();
        } else {
            tickUntamedFlight();
        }
    }

    private void clientTick() {}


    private void tickAngryChase() {
        LivingEntity target = getTarget();
        Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 toTarget = targetPos.subtract(position());
        double dist = toTarget.length();

        double aggroSpeed = GhostEntityData.FLY_SPEED * 2.5;
        double speed = Math.min(dist * 0.1, aggroSpeed);
        Vec3 desired = toTarget.normalize().scale(speed);

        smoothVelocity = lerpVec3(smoothVelocity, desired, GhostEntityData.SMOOTH_FACTOR * 3.0);
        setDeltaMovement(smoothVelocity);

        if (smoothVelocity.horizontalDistanceSqr() > 0.0001) {
            float targetYaw = (float) (Mth.atan2(smoothVelocity.z, smoothVelocity.x) * (180.0 / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.25F, getYRot(), targetYaw));
            yHeadRot = getYRot();
            yBodyRot = getYRot();
        }

        if (dist < 1.8 && tickCount % 20 == 0) {
            doHurtTarget(target);
        }
    }

    private void tickUntamedFlight() {
        graveListRefreshTimer--;
        if (graveListRefreshTimer <= 0) {
            refreshGraveList();
            graveListRefreshTimer = 600;
        }

        if (targetGravePos != null) {
            flyToTarget();
        } else {
            pickNextGrave();
            if (targetGravePos == null) {
                applyIdleHover();
            }
        }
    }

    private void flyToTarget() {
        Vec3 target = Vec3.atCenterOf(targetGravePos).add(0, GhostEntityData.HOVER_HEIGHT, 0);
        Vec3 toTarget = target.subtract(position());
        double dist = toTarget.horizontalDistance();

        if (dist < GhostEntityData.ARRIVAL_THRESHOLD && Math.abs(toTarget.y) < 2.0) {
            onArrivedAtGrave();
            return;
        }

        double speed = Math.min(dist * 0.06, GhostEntityData.FLY_SPEED);
        Vec3 desired = toTarget.normalize().scale(speed);

        double totalDist = homePos != null ? homePos.distSqr(targetGravePos) : dist * dist;
        double progress = 1.0 - (dist * dist / Math.max(totalDist, 1.0));
        if (progress < 0.4) {
            desired = desired.add(0, 0.02 * (1.0 - progress * 2.5), 0);
        }

        smoothVelocity = lerpVec3(smoothVelocity, desired, GhostEntityData.SMOOTH_FACTOR);
        setDeltaMovement(smoothVelocity);

        if (smoothVelocity.horizontalDistanceSqr() > 0.0001) {
            float targetYaw = (float) (Mth.atan2(smoothVelocity.z, smoothVelocity.x) * (180.0 / Math.PI)) - 90.0F;
            setYRot(Mth.rotLerp(0.12F, getYRot(), targetYaw));
            yHeadRot = getYRot();
            yBodyRot = getYRot();
        }
    }

    private void onArrivedAtGrave() {
        visitedGraves.add(targetGravePos);

        if (stealCooldown <= 0 && stolenItems.size() < GhostEntityData.MAX_STOLEN_ITEMS) {
            stealFromGraveAt(targetGravePos);
            stealCooldown = GhostEntityData.STEAL_DELAY_TICKS;
        }

        homePos = targetGravePos;
        targetGravePos = null;
        pickNextGrave();

        if (targetGravePos == null) {
            applyIdleHover();
        }
    }

    private void stealFromGraveAt(BlockPos pos) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        AABB searchBox = new AABB(pos).inflate(1.5);
        List<GraveStoneEntity> graves = serverLevel.getEntitiesOfClass(GraveStoneEntity.class, searchBox,
                e -> e.getBoundPos() != null && e.getBoundPos().distSqr(pos) < 4);

        if (graves.isEmpty()) return;

        GraveStoneEntity grave = graves.getFirst();
        ItemStack stolen = grave.stealRandomItem();

        if (!stolen.isEmpty()) {
            stolenItems.add(stolen);
        }
    }

    private void pickNextGrave() {
        if (cachedGraves.isEmpty()) {
            targetGravePos = null;
            return;
        }

        List<BlockPos> available = new ArrayList<>();
        for (BlockPos pos : cachedGraves) {
            if (!visitedGraves.contains(pos)) {
                available.add(pos);
            }
        }

        if (available.isEmpty()) {
            visitedGraves.clear();
            available.addAll(cachedGraves);
        }

        if (available.isEmpty()) {
            targetGravePos = null;
            return;
        }

        BlockPos myPos = blockPosition();
        available.sort(Comparator.comparingDouble(p -> p.distSqr(myPos)));

        int maxIndex = Math.min(available.size(), 5);
        targetGravePos = available.get(random.nextInt(maxIndex));
    }

    private void refreshGraveList() {
        CemeteryManager manager = CemeteryManager.getInstance();
        BlockPos pos = homePos != null ? homePos : blockPosition();
        Set<BlockPos> graves = manager.getClusterGraves(level().dimension(), pos);

        if (graves == null || graves.isEmpty()) {
            graves = manager.getGravesInRadius(level().dimension(), blockPosition(), 48);
        }

        cachedGraves = graves != null ? new ArrayList<>(graves) : new ArrayList<>();
    }

    private void applyIdleHover() {
        double bobY = Math.sin(tickCount * 0.05) * 0.008;
        Vec3 idle = new Vec3(0, bobY, 0);
        smoothVelocity = lerpVec3(smoothVelocity, idle, 0.05);
        setDeltaMovement(smoothVelocity);
    }


    private void tickTamedBehavior() {
        switch (behaviorMode) {
            case FOLLOW -> tickFollow();
            case WANDER -> tickWander();
            case STAY -> tickStay();
        }
    }

    private void tickFollow() {
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) {
            applyIdleHover();
            return;
        }

        double dist = distanceTo(owner);

        if (dist > 40.0) {
            teleportTo(owner.getX(), owner.getY(), owner.getZ());
            return;
        }

        if (dist > 3.0) {
            double angle = (getId() % 6) * (Math.PI * 2.0 / 6.0);
            double offsetX = Math.cos(angle) * 1.8;
            double offsetZ = Math.sin(angle) * 1.8;
            Vec3 target = owner.position().add(offsetX, 1.5, offsetZ);
            Vec3 toOwner = target.subtract(position());
            double speed = Math.min(dist * 0.04, GhostEntityData.FLY_SPEED * 1.2);
            Vec3 desired = toOwner.normalize().scale(speed);

            smoothVelocity = lerpVec3(smoothVelocity, desired, GhostEntityData.SMOOTH_FACTOR * 1.5);
            setDeltaMovement(smoothVelocity);

            if (smoothVelocity.horizontalDistanceSqr() > 0.0001) {
                float targetYaw = (float) (Mth.atan2(smoothVelocity.z, smoothVelocity.x) * (180.0 / Math.PI)) - 90.0F;
                setYRot(Mth.rotLerp(0.15F, getYRot(), targetYaw));
                yHeadRot = getYRot();
                yBodyRot = getYRot();
            }
        } else {
            applyIdleHover();
        }
    }

    private void tickWander() {
        if (targetGravePos == null || tickCount % 200 == 0) {
            int rx = random.nextInt(16) - 8;
            int rz = random.nextInt(16) - 8;
            targetGravePos = blockPosition().offset(rx, 0, rz);
        }

        Vec3 target = Vec3.atCenterOf(targetGravePos).add(0, 1.5, 0);
        Vec3 toTarget = target.subtract(position());
        double dist = toTarget.horizontalDistance();

        if (dist < 2.0) {
            applyIdleHover();
            return;
        }

        Vec3 desired = toTarget.normalize().scale(GhostEntityData.FLY_SPEED * 0.5);
        smoothVelocity = lerpVec3(smoothVelocity, desired, GhostEntityData.SMOOTH_FACTOR * 0.8);
        setDeltaMovement(smoothVelocity);
    }

    private void tickStay() {
        applyIdleHover();
    }


    private void updateTamedMood() {
        if (shyTimer > 0) return;

        if (getTarget() != null && getTarget().isAlive()) {
            setMood(GhostMood.ANGRY);
            return;
        }

        updateMoodFromState();
    }

    private void updateMoodFromState() {
        switch (behaviorMode) {
            case FOLLOW -> {
                Player owner = getOwner();
                if (owner != null && distanceTo(owner) < 20.0) {
                    setMood(GhostMood.HAPPY);
                } else {
                    setMood(GhostMood.SAD);
                }
            }
            case WANDER -> setMood(GhostMood.SAD);
            case STAY -> setMood(GhostMood.NEUTRAL);
        }
    }

    private void updateUntamedMood() {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !target.isSpectator() && !(target instanceof Player p && p.isCreative())) {
            setMood(GhostMood.ANGRY);
        } else {
            if (target != null) {
                setTarget(null);
                targetGravePos = null;
                pickNextGrave();
            }
            setMood(GhostMood.DEFAULT);
        }
    }


    private void spawnSoulParticleBurst() {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SOUL, getX(), getY() + getBbHeight() * 0.5, getZ(), 15, 0.4, 0.5, 0.4, 0.02);
        }
    }


    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level().isClientSide()) {
            if (player.isShiftKeyDown() && isTame() && isOwnedBy(player)) {
                cycleBehaviorMode(player);
                return InteractionResult.SUCCESS;
            }

            if (stack.is(Items.GLOWSTONE_DUST)) {
                return handleGlowstoneFeed(player, stack);
            }

            if (stack.isEmpty() && isTame() && isOwnedBy(player)) {
                return handlePet(player);
            }
        }

        return super.mobInteract(player, hand);
    }

    private InteractionResult handleGlowstoneFeed(Player player, ItemStack stack) {
        if (isTame()) {
            return InteractionResult.PASS;
        }

        consumeItem(player, stack);
        feedCount++;

        if (feedCount >= GhostEntityData.FEEDS_TO_TAME) {
            tame(player);
            setTarget(null);
            feedCount = 0;
            behaviorMode = BehaviorMode.FOLLOW;
            setMood(GhostMood.HAPPY);

            dropStolenItems();

            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("yagm.ghost.tamed"), true);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private InteractionResult handlePet(Player player) {
        shyTimer = GhostEntityData.SHY_DURATION_TICKS;
        setMood(GhostMood.SHY);

        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.3, getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }

        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("yagm.ghost.petted"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private void cycleBehaviorMode(Player player) {
        behaviorMode = behaviorMode.next();
        targetGravePos = null;

        updateMoodFromState();

        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable(behaviorMode.getTranslationKey()), true);
        }
    }


    private void consumeItem(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }


    public void dropStolenItems() {
        if (level().isClientSide()) return;

        for (ItemStack item : stolenItems) {
            if (!item.isEmpty()) {
                spawnAtLocation(item.copy());
            }
        }
        stolenItems.clear();
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        dropStolenItems();
        super.dropAllDeathLoot(level, source);
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isTame() && source.getEntity() instanceof Player player && isOwnedBy(player)) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result && !level().isClientSide()) {
            setMood(GhostMood.ANGRY);
            smoothVelocity = getDeltaMovement();
        }
        return result;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isTame();
    }

    public static boolean canGhostSpawn(Level level, BlockPos pos) {
        if (level.isDay()) return false;

        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        if (blockLight > 7) return false;

        return CemeteryManager.getInstance().isCemetery(level.dimension(), pos);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Mood", getMoodTextureName());
        tag.putBoolean("Tame", isTame());
        tag.putInt("BehaviorMode", behaviorMode.ordinal());
        tag.putInt("FeedCount", feedCount);

        UUID ownerUuid = getOwnerUUID();
        if (ownerUuid != null) {
            tag.putUUID("OwnerUUID", ownerUuid);
        }

        if (homePos != null) {
            tag.putLong("HomePos", homePos.asLong());
        }

        if (!stolenItems.isEmpty()) {
            ListTag stolenTag = new ListTag();
            for (ItemStack item : stolenItems) {
                if (!item.isEmpty()) {
                    stolenTag.add(item.save(level().registryAccess()));
                }
            }
            tag.put("StolenItems", stolenTag);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (tag.contains("Mood")) setMood(GhostMood.fromString(tag.getString("Mood")));
        if (tag.contains("Tame")) setTame(tag.getBoolean("Tame"));
        if (tag.contains("FeedCount")) feedCount = tag.getInt("FeedCount");

        if (tag.contains("BehaviorMode")) {
            int ordinal = tag.getInt("BehaviorMode");
            BehaviorMode[] values = BehaviorMode.values();
            behaviorMode = ordinal >= 0 && ordinal < values.length ? values[ordinal] : BehaviorMode.FOLLOW;
        }

        if (tag.hasUUID("OwnerUUID")) {
            setOwnerUUID(tag.getUUID("OwnerUUID"));
        }

        if (tag.contains("HomePos")) {
            homePos = BlockPos.of(tag.getLong("HomePos"));
        }


        stolenItems.clear();
        if (tag.contains("StolenItems", Tag.TAG_LIST)) {
            ListTag stolenTag = tag.getList("StolenItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < stolenTag.size(); i++) {
                ItemStack item = ItemStack.parseOptional(level().registryAccess(), stolenTag.getCompound(i));
                if (!item.isEmpty()) {
                    stolenItems.add(item);
                }
            }
        }
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return isTame() || getMood() == GhostMood.ANGRY;
    }

    @Override
    protected void pushEntities() {
        if (isTame()) {
            super.pushEntities();
        }
    }


    private static Vec3 lerpVec3(Vec3 current, Vec3 target, double factor) {
        return new Vec3(
                Mth.lerp(factor, current.x, target.x),
                Mth.lerp(factor, current.y, target.y),
                Mth.lerp(factor, current.z, target.z)
        );
    }


    static class GhostMoveControl extends MoveControl {
        private final GhostEntity ghost;

        GhostMoveControl(GhostEntity ghost) {
            super(ghost);
            this.ghost = ghost;
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                Vec3 target = new Vec3(wantedX, wantedY, wantedZ);
                Vec3 toTarget = target.subtract(ghost.position());
                double dist = toTarget.length();

                if (dist < 0.5) {
                    this.operation = Operation.WAIT;
                    ghost.setDeltaMovement(ghost.getDeltaMovement().scale(0.5));
                    return;
                }

                Vec3 desired = toTarget.normalize().scale(Math.min(dist * 0.05, speedModifier * 0.15));
                ghost.smoothVelocity = lerpVec3(ghost.smoothVelocity, desired, GhostEntityData.SMOOTH_FACTOR);
                ghost.setDeltaMovement(ghost.smoothVelocity);

                if (toTarget.horizontalDistanceSqr() > 0.01) {
                    float yaw = (float) (Mth.atan2(toTarget.z, toTarget.x) * (180.0 / Math.PI)) - 90.0F;
                    ghost.setYRot(Mth.rotLerp(0.12F, ghost.getYRot(), yaw));
                    ghost.yHeadRot = ghost.getYRot();
                    ghost.yBodyRot = ghost.getYRot();
                }
            }
        }
    }

    static class GhostAggroGoal extends NearestAttackableTargetGoal<Player> {
        private final GhostEntity ghost;

        GhostAggroGoal(GhostEntity ghost) {
            super(ghost, Player.class, true);
            this.ghost = ghost;
        }

        @Override
        public boolean canUse() {
            if (ghost.isTame()) return false;

            CemeteryManager manager = CemeteryManager.getInstance();
            BlockPos ghostPos = ghost.blockPosition();
            boolean nearCemetery = manager.isCemetery(ghost.level().dimension(), ghostPos) || manager.getGraveCountNear(ghost.level().dimension(), ghostPos, (int) GhostEntityData.CEMETERY_AGGRO_RADIUS) > 0;

            return nearCemetery && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            if (ghost.isTame()) return false;
            LivingEntity target = ghost.getTarget();
            if (target instanceof Player p && (p.isCreative() || p.isSpectator())) {
                ghost.setTarget(null);
                return false;
            }
            return super.canContinueToUse();
        }
    }
}
