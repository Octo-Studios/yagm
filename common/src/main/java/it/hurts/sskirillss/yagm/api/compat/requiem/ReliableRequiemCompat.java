package it.hurts.sskirillss.yagm.api.compat.requiem;

import dev.architectury.event.EventResult;
import dev.architectury.platform.Platform;
import it.hurts.sskirillss.yagm.api.event.IServerEvent;
import it.hurts.sskirillss.yagm.event.death.service.GraveDeathService;
import it.hurts.sskirillss.yagm.init.ConfigRegistry;
import it.hurts.sskirillss.yagm.util.BlacklistUtils;
import it.hurts.sskirillss.yagm.util.DroppedInventory;
import it.hurts.sskirillss.yagm.util.GravePayloadUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReliableRequiemCompat {
    private static final Map<UUID, DeathDrops> DEATH_DROPS = new HashMap<>();

    public static void init() {
        IServerEvent.BEFORE_DEATH_CAPTURE.register(player -> isEnabled()
                && !player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
                ? EventResult.interruptFalse() : EventResult.pass());
    }

    public static boolean isEnabled() {
        return Platform.isModLoaded("reliable_requiem") && ConfigRegistry.isCompatEnabled("reliable_requiem");
    }

    public static void handleDeathDrops(ServerPlayer player, Runnable dropLoot) {
        var inventory = new DroppedInventory(player);
        var drops = new DeathDrops();
        DEATH_DROPS.put(player.getUUID(), drops);
        var handled = false;

        try {
            try {
                dropLoot.run();
            } finally {
                DEATH_DROPS.remove(player.getUUID());
            }

            var graveData = inventory.save(player, drops.entities, drops.experience);
            handled = !GravePayloadUtils.hasRecoverablePayload(graveData) || GraveDeathService.handlePrepared(player, graveData);
        } finally {
            if (!handled) {
                drops.entities.forEach(drop -> drop.level().addFreshEntity(drop));

                if (drops.experience > 0) {
                    ExperienceOrb.award(player.serverLevel(), player.position(), drops.experience);
                }
            }
        }
    }

    public static boolean captureDrop(Entity owner, ItemEntity drop) {
        var drops = DEATH_DROPS.get(owner.getUUID());

        if (drops == null || drop.getItem().isEmpty() || BlacklistUtils.isBlacklisted(drop.getItem())) {
            return false;
        }

        drops.entities.add(drop);
        return true;
    }

    public static boolean captureExperience(Entity entity, int amount) {
        var drops = DEATH_DROPS.get(entity.getUUID());

        if (drops == null) {
            return false;
        }

        drops.experience += amount;
        return true;
    }

    private static final class DeathDrops {
        private final List<ItemEntity> entities = new ArrayList<>();
        private int experience;
    }
}
