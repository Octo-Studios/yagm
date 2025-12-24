package it.hurts.sskirillss.yagm.data_components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record GraveStoneListComponents(List<ItemStack> list) {
    public static final Codec<GraveStoneListComponents> CODEC = RecordCodecBuilder.create(instance -> instance.group(ItemStack.CODEC.listOf().fieldOf("_gravestone").forGetter(GraveStoneListComponents::list)
    ).apply(instance, GraveStoneListComponents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GraveStoneListComponents> STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()), GraveStoneListComponents::list, GraveStoneListComponents::new);
}