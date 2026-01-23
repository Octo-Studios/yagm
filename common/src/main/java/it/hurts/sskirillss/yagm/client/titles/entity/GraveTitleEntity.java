package it.hurts.sskirillss.yagm.client.titles.entity;

import it.hurts.sskirillss.yagm.blocks.gravestones.gravestone.GraveStoneBlockEntity;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;


@Setter
@Getter
public class GraveTitleEntity extends Entity {

    private static final EntityDataAccessor<Component> DATA_TEXT = SynchedEntityData.defineId(GraveTitleEntity.class, EntityDataSerializers.COMPONENT);

    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(GraveTitleEntity.class, EntityDataSerializers.INT);

    private BlockPos linkedGravePos;

    public GraveTitleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        builder.define(DATA_TEXT, Component.empty());
        builder.define(DATA_COLOR, 0xFFFFFF);
    }

    public void setText(Component text) {
        this.entityData.set(DATA_TEXT, text);
    }

    public Component getText() {
        return this.entityData.get(DATA_TEXT);
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide() && linkedGravePos != null) {
            if (!(level().getBlockEntity(linkedGravePos) instanceof GraveStoneBlockEntity)) {
                this.discard();
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Text")) {
            setText(Component.Serializer.fromJson(tag.getString("Text"), registryAccess()));
        }
        if (tag.contains("Color")) {
            setColor(tag.getInt("Color"));
        }
        if (tag.contains("GraveX")) {
            linkedGravePos = new BlockPos(
                    tag.getInt("GraveX"),
                    tag.getInt("GraveY"),
                    tag.getInt("GraveZ")
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Text", Component.Serializer.toJson(getText(), registryAccess()));
        tag.putInt("Color", getColor());
        if (linkedGravePos != null) {
            tag.putInt("GraveX", linkedGravePos.getX());
            tag.putInt("GraveY", linkedGravePos.getY());
            tag.putInt("GraveZ", linkedGravePos.getZ());
        }
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}