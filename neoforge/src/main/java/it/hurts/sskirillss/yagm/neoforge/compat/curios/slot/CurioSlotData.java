package it.hurts.sskirillss.yagm.neoforge.compat.curios.slot;


import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.hurts.sskirillss.yagm.YAGMCommon;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


@Slf4j
public class CurioSlotData {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, YAGMCommon.MODID);

    public record SlotInfo(String slotType, int slotIndex, boolean wasEquipped, boolean isCosmetic) {}

    public static final Codec<SlotInfo> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("slotType").forGetter(SlotInfo::slotType),
                    Codec.INT.fieldOf("slotIndex").forGetter(SlotInfo::slotIndex),
                    Codec.BOOL.fieldOf("wasEquipped").forGetter(SlotInfo::wasEquipped),
                    Codec.BOOL.optionalFieldOf("isCosmetic", false).forGetter(SlotInfo::isCosmetic)
            ).apply(instance, SlotInfo::new));


    public static final StreamCodec<RegistryFriendlyByteBuf, SlotInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SlotInfo::slotType,
            ByteBufCodecs.VAR_INT, SlotInfo::slotIndex,
            ByteBufCodecs.BOOL, SlotInfo::wasEquipped,
            ByteBufCodecs.BOOL, SlotInfo::isCosmetic,
            SlotInfo::new);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SlotInfo>> CURIO_SLOT_DATA = DATA_COMPONENTS.register("curio_slot_data", () -> DataComponentType.<SlotInfo>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
        log.info("[YAGM] CurioSlotData component type registered: {}", CURIO_SLOT_DATA.getId());
    }
}