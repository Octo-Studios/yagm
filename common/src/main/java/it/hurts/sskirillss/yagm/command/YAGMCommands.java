package it.hurts.sskirillss.yagm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.hurts.sskirillss.yagm.YAGMCommon;
import it.hurts.sskirillss.yagm.data.GraveSaveManager;
import it.hurts.sskirillss.yagm.network.handlers.InventoryHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;
import java.util.UUID;

public class YAGMCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("yagm")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("restore")
                    .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            context.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(player -> player.getName().getString())
                                .filter(name -> name.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("save", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                String playerName = StringArgumentType.getString(context, "player");
                                ServerLevel level = context.getSource().getLevel();
                                String worldName = level.getServer().getWorldPath(LevelResource.ROOT).getFileName().toString();
                                
                                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                                if (targetPlayer != null) {
                                    List<String> saves = GraveSaveManager.listSaves(worldName, targetPlayer.getUUID());
                                    saves.stream()
                                        .filter(save -> save.toLowerCase().startsWith(builder.getRemaining().toLowerCase()))
                                        .forEach(builder::suggest);
                                }
                                return builder.buildFuture();
                            })
                            .executes(YAGMCommands::restoreGrave)
                        )
                    )
                )
        );
    }

    private static int restoreGrave(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        String saveName = StringArgumentType.getString(context, "save");
        if (!saveName.endsWith(".dat")) {
            saveName += ".dat";
        }
        String finalSaveName = saveName;

        ServerLevel level = context.getSource().getLevel();
        ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayerByName(playerName);

        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        String worldName = level.getServer().getWorldPath(LevelResource.ROOT).getFileName().toString();
        CompoundTag graveData = GraveSaveManager.loadGraveData(worldName, targetPlayer.getUUID(), saveName);

        if (graveData == null) {
            context.getSource().sendFailure(Component.literal("Save not found: " + saveName));
            return 0;
        }

        try {
            InventoryHelper.restoreFromNBT(targetPlayer, graveData, true);
            context.getSource().sendSuccess(() -> Component.literal("Successfully restored grave for player " + playerName + " from save " + finalSaveName), true);
            YAGMCommon.LOGGER.info("Restored grave data for player {} from save {}", playerName, finalSaveName);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to restore grave: " + e.getMessage()));
            YAGMCommon.LOGGER.error("Failed to restore grave for player {} from save {}: {}", playerName, saveName, e.getMessage(), e);
            return 0;
        }
    }
}
