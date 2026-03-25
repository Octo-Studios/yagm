package it.hurts.sskirillss.yagm.api.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;


public interface IServerEvent {

    /**
     * Called when a player dies and a grave needs to be created.
     * Inventory data is already collected in graveData.
     */
    Event<OnPlayerDeath> ON_PLAYER_DEATH = EventFactory.createEventResult();

    /**
     *Called AFTER successfully placing a grave.
     */
    Event<OnGravePlaced> ON_GRAVE_PLACED = EventFactory.createLoop();


    @FunctionalInterface
    public interface OnPlayerDeath {
        EventResult onPlayerDeath(ServerPlayer player, CompoundTag graveData);
    }

    @FunctionalInterface
    public interface OnGravePlaced {
        EventResult onGravePlaced(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player, CompoundTag graveData);
    }
}