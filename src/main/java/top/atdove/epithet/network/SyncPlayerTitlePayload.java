package top.atdove.epithet.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.client.ClientTitleManager;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record SyncPlayerTitlePayload(
    UUID playerId,
    Optional<ResourceLocation> activeTitle,
    Set<ResourceLocation> unlockedTitles,
    boolean isLocked,
    boolean isSelf
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncPlayerTitlePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "sync_player_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncPlayerTitlePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, payload.playerId);
            buf.writeBoolean(payload.activeTitle.isPresent());
            payload.activeTitle.ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
            buf.writeVarInt(payload.unlockedTitles.size());
            for (ResourceLocation id : payload.unlockedTitles) {
                ResourceLocation.STREAM_CODEC.encode(buf, id);
            }
            buf.writeBoolean(payload.isLocked);
            buf.writeBoolean(payload.isSelf);
        },
        buf -> {
            UUID playerId = UUIDUtil.STREAM_CODEC.decode(buf);
            Optional<ResourceLocation> active = buf.readBoolean()
                ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
                : Optional.empty();
            int count = buf.readVarInt();
            Set<ResourceLocation> unlocked = new HashSet<>(count);
            for (int i = 0; i < count; i++) {
                unlocked.add(ResourceLocation.STREAM_CODEC.decode(buf));
            }
            boolean locked = buf.readBoolean();
            boolean isSelf = buf.readBoolean();
            return new SyncPlayerTitlePayload(playerId, active, unlocked, locked, isSelf);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncPlayerTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (FMLEnvironment.dist.isClient()) {
                ClientTitleManager.handleSyncPlayerTitle(payload);
            }
        });
    }
}
