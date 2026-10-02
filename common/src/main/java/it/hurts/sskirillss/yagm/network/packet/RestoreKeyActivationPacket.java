package it.hurts.sskirillss.yagm.network.packet;

import it.hurts.sskirillss.yagm.YAGMCommon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record RestoreKeyActivationPacket(ItemStack stack) implements CustomPacketPayload {
    public static final Type<RestoreKeyActivationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(YAGMCommon.MODID, "restore_key_activation"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RestoreKeyActivationPacket> CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, RestoreKeyActivationPacket::stack, RestoreKeyActivationPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
