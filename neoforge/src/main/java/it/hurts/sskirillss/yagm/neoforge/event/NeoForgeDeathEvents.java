package it.hurts.sskirillss.yagm.neoforge.event;

import it.hurts.sskirillss.yagm.api.compat.twilight.TwilightForestCompat;
import it.hurts.sskirillss.yagm.event.GraveStoneEvent;
import it.hurts.sskirillss.yagm.event.death.tracker.GraveDeathTracker;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NeoForgeDeathEvents {

    private static final Map<UUID, DInventory> SFD = new ConcurrentHashMap<>();
    private static boolean initialized;

    public static void register() {
        if (initialized) return;

        initialized = true;
        GraveDeathTracker.getdeathhandler();

        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeoForgeDeathEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, NeoForgeDeathEvents::onLivingDrops);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (event.isCanceled()
                || TwilightForestCompat.isPOST_DEATH()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || player instanceof FakePlayer
                || player.isSpectator()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        SFD.put(player.getUUID(), new DInventory(player.serverLevel().getGameTime(), InventoryUtils.savePlayerInventory(player)));
    }

    private static void onLivingDrops(LivingDropsEvent event) {
        Set<ItemEntity> accessoryDrops = Set.of();

        if (event.getEntity() instanceof ServerPlayer player) {
            accessoryDrops = NeoForgeDropTracker.consume(player.getUUID());
        }

        if (TwilightForestCompat.isPOST_DEATH()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()
                || player instanceof FakePlayer
                || player.isSpectator()
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
            return;
        }

        DInventory pending = SFD.remove(player.getUUID());

        CompoundTag inventory = null;

        if (pending != null && player.serverLevel().getGameTime() - pending.gameTime() <= 2L) {
            inventory = pending.data();
        }

        Collection<ItemEntity> graveDrops = event.getDrops();

        if (inventory != null && !accessoryDrops.isEmpty()) {
            graveDrops = new ArrayList<>(graveDrops);
            graveDrops.removeIf(accessoryDrops::contains);
        }

        if (GraveStoneEvent.handlePlayerDeathDrops(player, graveDrops, inventory)) {
            event.getDrops().clear();
            player.skipDropExperience();
        }
    }

    private record DInventory(long gameTime, CompoundTag data) { }
}
