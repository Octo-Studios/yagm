package it.hurts.sskirillss.yagm.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import it.hurts.sskirillss.yagm.client.overlays.GraveInventoryHudRenderer;
import it.hurts.sskirillss.yagm.client.particle.FireParticle;
import it.hurts.sskirillss.yagm.client.particle.GhostlyFogParticle;
import it.hurts.sskirillss.yagm.client.particle.GroundDustParticle;
import it.hurts.sskirillss.yagm.client.network.RestoreKeyActivationHandler;
import it.hurts.sskirillss.yagm.client.renderer.FallingGraveEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GhostEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GhostlyFogEntityRenderer;
import it.hurts.sskirillss.yagm.client.renderer.GraveStoneBlockEntityRenderer;
import it.hurts.sskirillss.yagm.init.BlockEntityRegistry;
import it.hurts.sskirillss.yagm.init.EntityRegistry;
import it.hurts.sskirillss.yagm.init.ParticleRegistry;
import it.hurts.sskirillss.yagm.network.packet.RestoreKeyActivationPacket;

public class YAGMClient {

    public static void init() {
        registerEntityRenderers();
        registerParticleProviders();
        registerPacket();
        ClientGuiEvent.RENDER_HUD.register(GraveInventoryHudRenderer::onRenderHud);
    }

    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(EntityRegistry.FALLING_GRAVE, FallingGraveEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GHOST, GhostEntityRenderer::new);
        EntityRendererRegistry.register(EntityRegistry.GHOSTLY_FOG, GhostlyFogEntityRenderer::new);
        BlockEntityRendererRegistry.register(BlockEntityRegistry.GRAVE_STONE.get(), GraveStoneBlockEntityRenderer::new);
    }

    private static void registerParticleProviders() {
        ParticleProviderRegistry.register(ParticleRegistry.GRAVE_DUST_FLAT.get(), GroundDustParticle.Provider::new);
        ParticleProviderRegistry.register(ParticleRegistry.GHOSTLY_FOG.get(), GhostlyFogParticle.Provider::new);
        ParticleProviderRegistry.register(ParticleRegistry.CANDLE_FLAME.get(), FireParticle.Provider::new);
    }

    private static void registerPacket() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, RestoreKeyActivationPacket.TYPE, RestoreKeyActivationPacket.CODEC,
                (packet, context) -> {
                    context.queue(() -> RestoreKeyActivationHandler.handle(packet));
                }
        );
    }
}
