package it.hurts.sskirillss.yagm.neoforge.event;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NeoForgeDropTracker {

    private static final Map<UUID, Set<ItemEntity>> ACCESSORY_DROPS = new ConcurrentHashMap<>();

    public static void trackAccessoryDrops(UUID playerId, Collection<ItemEntity> drops) {
        if (drops.isEmpty()) {
            return;
        }

        Set<ItemEntity> tracked = Collections.newSetFromMap(new IdentityHashMap<>());
        tracked.addAll(drops);

        ACCESSORY_DROPS.merge(playerId, tracked, (current, added) -> {
            current.addAll(added);
            return current;
        });
    }

    public static Set<ItemEntity> consume(UUID playerId) {
        Set<ItemEntity> tracked = ACCESSORY_DROPS.remove(playerId);
        if (tracked == null){
            return Set.of();
        }
        else{
            return tracked;
        }
    }
}
