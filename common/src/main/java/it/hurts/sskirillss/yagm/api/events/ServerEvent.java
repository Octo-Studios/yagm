package it.hurts.sskirillss.yagm.api.events;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import it.hurts.sskirillss.yagm.blocks.gravestones.GraveStoneBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public interface ServerEvent {
    Event<OnPlayerDeathEvent> ON_PLAYER_DEATH = EventFactory.createLoop();
    Event<OnGravePlacedEvent> ON_GRAVE_PLACED = EventFactory.createLoop();

    @FunctionalInterface
    interface OnPlayerDeathEvent {
        /**
         * Called then player dead
         *
         * @param player Killed player
         * @param graveData NBT data of inventory
         * @return EventResult (you can change back result INTERRUPT)
         */
        EventResult onPlayerDeath(ServerPlayer player, CompoundTag graveData);
    }

    @FunctionalInterface
    interface OnGravePlacedEvent {
        /**
         * Called when grave stone is placed in the world
         *
         * @param level Server level where grave was placed
         * @param pos Position of the grave
         * @param graveEntity The grave block entity
         * @param player Player who died
         * @return EventResult (you can use INTERRUPT to prevent grave placement)
         */
        EventResult onGravePlaced(ServerLevel level, BlockPos pos, GraveStoneBlockEntity graveEntity, ServerPlayer player);
    }
}