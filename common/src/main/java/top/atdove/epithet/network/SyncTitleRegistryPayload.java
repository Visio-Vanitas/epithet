package top.atdove.epithet.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.ArrayList;
import java.util.List;

public record SyncTitleRegistryPayload(List<TitleDefinition> titles) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncTitleRegistryPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "sync_title_registry"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncTitleRegistryPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.titles.size());
            for (TitleDefinition def : payload.titles) {
                TitleDefinition.STREAM_CODEC.encode(buf, def);
            }
        },
        buf -> {
            int size = buf.readVarInt();
            List<TitleDefinition> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(TitleDefinition.STREAM_CODEC.decode(buf));
            }
            return new SyncTitleRegistryPayload(list);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncTitleRegistryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            TitleRegistry.getInstance().setClientTitles(payload.titles());
        });
    }
}
