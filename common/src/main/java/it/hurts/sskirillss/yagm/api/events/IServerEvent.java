package it.hurts.sskirillss.yagm.api.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface IServerEvent {

    /**
     * Called when a player dies and a grave needs to be created.
     * Inventory data is already collected in graveData.
     */
    Event<OnPlayerDeath> ON_PLAYER_DEATH = EventFactory.createEventResult();

    /**
     * Called BEFORE placing the grave.
     * Return EventResult.interruptFalse() to cancel.
     */
    Event<OnGravePlacing> ON_GRAVE_PLACING = EventFactory.createEventResult();

    /**
     *Called AFTER successfully placing a grave.
     */
    Event<OnGravePlaced> ON_GRAVE_PLACED = EventFactory.createLoop();

    /**
     * Called when the player interacts with a grave.
     * Return EventResult.interruptFalse() to cancel.
     */
    Event<OnGraveInteract> ON_GRAVE_INTERACT = EventFactory.createEventResult();

    /**
     * Called when a grave is destroyed.
     * Return EventResult.interruptFalse() to cancel.
     */
    Event<OnGraveBreaking> ON_GRAVE_BREAKING = EventFactory.createEventResult();

    /**
     * Called after restoring inventory from the grave.
     */
    Event<OnGraveRestored> ON_GRAVE_RESTORED = EventFactory.createLoop();


    @FunctionalInterface
    public interface OnPlayerDeath {
        EventResult onPlayerDeath(ServerPlayer player, CompoundTag graveData);
    }

    @FunctionalInterface
    public interface OnGravePlacing {
        EventResult onGravePlacing(ServerLevel level, BlockPos pos, ServerPlayer player, CompoundTag graveData);
    }

    @FunctionalInterface
    public interface OnGravePlaced {
        EventResult onGravePlaced(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, CompoundTag graveData);
    }

    @FunctionalInterface
    public interface OnGraveInteract {
        EventResult onGraveInteract(ServerLevel level, BlockPos pos, ServerPlayer player);
    }

    @FunctionalInterface
    public interface OnGraveBreaking {
        EventResult onGraveBreaking(ServerLevel level, BlockPos pos, ServerPlayer player);
    }

    @FunctionalInterface
    public interface OnGraveRestored {
        void onGraveRestored(ServerLevel level, BlockPos pos, ServerPlayer player);
    }
}