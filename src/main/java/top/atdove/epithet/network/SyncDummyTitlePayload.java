package top.atdove.epithet.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.client.ClientDummyManager;

import java.util.Optional;
import java.util.UUID;

public record SyncDummyTitlePayload(
    int entityId,
    UUID entityUuid,
    Optional<ResourceLocation> titleId,
    boolean isDummy
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncDummyTitlePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "sync_dummy_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDummyTitlePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeVarInt(payload.entityId);
            UUIDUtil.STREAM_CODEC.encode(buf, payload.entityUuid);
            buf.writeBoolean(payload.titleId.isPresent());
            payload.titleId.ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
            buf.writeBoolean(payload.isDummy);
        },
        buf -> {
            int entityId = buf.readVarInt();
            UUID entityUuid = UUIDUtil.STREAM_CODEC.decode(buf);
            Optional<ResourceLocation> titleId = buf.readBoolean()
                ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
                : Optional.empty();
            boolean isDummy = buf.readBoolean();
            return new SyncDummyTitlePayload(entityId, entityUuid, titleId, isDummy);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDummyTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ClientDummyManager.handleSync(payload);
            }
        });
    }
}
