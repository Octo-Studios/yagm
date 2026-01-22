package it.hurts.sskirillss.yagm.register;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import it.hurts.sskirillss.yagm.command.YAGMCommands;

public class CommandRegistry {

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) -> {
            YAGMCommands.register(dispatcher);
        });
    }
}