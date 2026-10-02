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
public final class CurioSlotData {

    private static final DeferredRegister<DataComponentType<?>> COMPONENT_TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, YAGMCommon.MODID);

    public record SlotInfo(boolean cosmetic, boolean equipped, int slotIndex, String slotType) {

        public SlotInfo(String slotType, int slotIndex, boolean wasEquipped, boolean isCosmetic) {
            this(isCosmetic, wasEquipped, slotIndex, slotType);
        }

        public boolean wasEquipped() {
            return equipped;
        }

        public boolean isCosmetic() {
            return cosmetic;
        }
    }

    private static final Codec<SlotInfo> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Codec.STRING.fieldOf("slotType").forGetter(SlotInfo::slotType),
            Codec.INT.fieldOf("slotIndex").forGetter(SlotInfo::slotIndex),
            Codec.BOOL.fieldOf("wasEquipped").forGetter(SlotInfo::wasEquipped),
            Codec.BOOL.optionalFieldOf("isCosmetic", false).forGetter(SlotInfo::isCosmetic)).apply(builder, SlotInfo::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, SlotInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SlotInfo::slotType,
            ByteBufCodecs.VAR_INT, SlotInfo::slotIndex,
            ByteBufCodecs.BOOL, SlotInfo::wasEquipped,
            ByteBufCodecs.BOOL, SlotInfo::isCosmetic,
            SlotInfo::new);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SlotInfo>> CURIO_SLOT_DATA =
            COMPONENT_TYPES.register("curio_slot_data", () -> DataComponentType.<SlotInfo>builder().persistent(CODEC).networkSynchronized(STREAM_CODEC).build());

    public static void register(IEventBus modEventBus) {
        COMPONENT_TYPES.register(modEventBus);
        log.debug("[YAGM] Registered curio slot data component '{}'", CURIO_SLOT_DATA.getId());
    }
}