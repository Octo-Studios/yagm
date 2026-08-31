package it.hurts.sskirillss.yagm.client.network;

import it.hurts.sskirillss.yagm.network.packet.RestoreKeyActivationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;

public final class RestoreKeyActivationHandler {

    public static void handle(RestoreKeyActivationPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.gameRenderer.displayItemActivation(packet.stack());

        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        }
    }
}
