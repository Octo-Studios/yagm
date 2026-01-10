package it.hurts.sskirillss.yagm.test;


import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.structures.cemetery.CemeteryManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class CemeteryTestLogger {

    public static void init(MinecraftServer server) {
        CemeteryManager.getInstance().setOnCemeteryFormed((dimension, center, graveCount) -> {
            String message = String.format("§6[YAGM] §eCemetery formed! §fLocation: §a%d, %d, %d §f| Graves: §a%d §f| Dimension: §a%s", center.getX(), center.getY(), center.getZ(), graveCount, dimension.location().toString());

            YAGMCommon.LOGGER.info("Cemetery formed at {}, {}, {} with {} graves in {}", center.getX(), center.getY(), center.getZ(), graveCount, dimension.location());

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.sendSystemMessage(Component.literal(message));
            }
        });
    }
}