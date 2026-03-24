package it.hurts.sskirillss.yagm.test;


import it.hurts.sskirillss.yagm.structure.cemetery.CemeteryManager;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@Slf4j
public class CemeteryTestLogger {

    public static void init(MinecraftServer server) {
        CemeteryManager.getInstance().setOnCemeteryFormed((dimension, center, graveCount) -> {
            log.info("Dimension: {}", dimension.location());
            log.info("Center: {}, {}, {}", center.getX(), center.getY(), center.getZ());
            log.info("Grave Count: {}", graveCount);

            String message = String.format("§6[YAGM] §eCemetery formed! §fLocation: §a%d, %d, %d §f| Graves: §a%d §f| Dimension: §a%s", center.getX(), center.getY(), center.getZ(), graveCount, dimension.location().toString());

            log.info("Sending message to {} players", server.getPlayerList().getPlayers().size());

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message));
                log.info("Message sent to player: {}", player.getName().getString());
            }
        });

        log.info("CemeteryTestLogger initialized successfully");
    }
}