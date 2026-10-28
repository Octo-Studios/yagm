package it.hurts.sskirillss.yagm.event.death.service.helper;

import it.hurts.sskirillss.yagm.component.level.GraveStoneLevels;
import it.hurts.sskirillss.yagm.component.placement.GravePlacementPlan;
import it.hurts.sskirillss.yagm.component.placement.GravePlacementService;
import it.hurts.sskirillss.yagm.entity.FallingGraveEntity;
import it.hurts.sskirillss.yagm.util.PlaceableUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public final class Placer {

    public static boolean place(ServerLevel level, ServerPlayer player, CompoundTag graveData, GraveStoneLevels graveLevel, GravePlacementPlan plan) {

        if (plan.falling()) {
            FallingGraveEntity entity = FallingGraveEntity.create(
                    level,
                    plan.spawnPos(),
                    plan.velocity(),
                    graveData,
                    graveLevel,
                    player.getUUID(),
                    player.getName().getString(),
                    player.getDirection().getOpposite(),
                    plan.recovery()
            );

            if (level.addFreshEntity(entity)) {
                return true;
            }

            log.warn("Unable to spawn falling grave for {} at {} in {}; attempting immediate placement",
                    player.getGameProfile().getName(), plan.spawnPos(), level.dimension().location());

            BlockPos fallbackAnchor = plan.immediatePos() != null ? plan.immediatePos() : player.blockPosition();
            BlockPos fallbackPos = PlaceableUtils.getNear(level, fallbackAnchor, 4, PlaceableUtils.getGraveHeight(graveLevel));
            if (fallbackPos == null) {
                fallbackPos = fallbackAnchor;
            }

            return GravePlacementService.placeImmediate(
                    level,
                    fallbackPos,
                    graveData,
                    graveLevel,
                    player.getUUID(),
                    player.getName().getString(),
                    player.getDirection().getOpposite(),
                    true,
                    false
            );
        }

        return GravePlacementService.placeImmediate(
                level,
                plan.immediatePos(),
                graveData,
                graveLevel,
                player.getUUID(),
                player.getName().getString(),
                player.getDirection().getOpposite(),
                plan.strictPlacement(),
                plan.recovery()
        );
    }
}
