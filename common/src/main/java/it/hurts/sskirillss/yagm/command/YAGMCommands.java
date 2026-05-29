package it.hurts.sskirillss.yagm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import it.hurts.sskirillss.yagm.data.gravedata.GraveSaveManager;
import lombok.extern.slf4j.Slf4j;
import it.hurts.sskirillss.yagm.util.InventoryUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

@Slf4j
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
                        .then(Commands.argument("save", StringArgumentType.greedyString())
                            .suggests((context, builder) -> {
                                String playerName = StringArgumentType.getString(context, "player");
                                ServerLevel level = context.getSource().getLevel();
                                
                                ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
                                if (targetPlayer != null) {
                                    List<CompoundTag> saves = GraveSaveManager.listSaves(level, targetPlayer.getUUID());
                                    String remaining = builder.getRemaining();
                                    for (CompoundTag save : saves) {
                                        String displayName = GraveSaveManager.formatTime(save);
                                        if (displayName.startsWith(remaining)) {
                                            builder.suggest(displayName, Component.literal(playerName + " " + displayName));
                                        }
                                    }
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
        String selector = StringArgumentType.getString(context, "save").trim();

        ServerLevel level = context.getSource().getLevel();
        ServerPlayer targetPlayer = level.getServer().getPlayerList().getPlayerByName(playerName);

        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("Player not found: " + playerName));
            return 0;
        }

        CompoundTag graveData = GraveSaveManager.loadGraveData(level, targetPlayer.getUUID(), selector);
        if (graveData == null) {
            context.getSource().sendFailure(Component.literal("Save not found: " + selector));
            return 0;
        }

        try {
            InventoryUtils.restoreFullGrave(targetPlayer, graveData);

            context.getSource().sendSuccess(() -> Component.literal("Grave restored for player " + playerName), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to restore grave: " + e.getMessage()));
            return 0;
        }
    }
}
