package top.atdove.epithet.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;

import java.util.Optional;

public record EquipTitlePayload(Optional<ResourceLocation> titleId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<EquipTitlePayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "equip_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EquipTitlePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBoolean(payload.titleId.isPresent());
            payload.titleId.ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
        },
        buf -> {
            Optional<ResourceLocation> id = buf.readBoolean()
                ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
                : Optional.empty();
            return new EquipTitlePayload(id);
        }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EquipTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
                if (data.isLocked()) {
                    Epithet.LOGGER.warn("Player {} tried to equip title while locked", player.getName().getString());
                    TitleNetworkHandler.syncPlayerTitle(player, player, true);
                    return;
                }
                if (payload.titleId().isEmpty()) {
                    data.setActiveTitle(Optional.empty());
                    TitleNetworkHandler.syncToPlayerAndTrackers(player);
                } else {
                    ResourceLocation id = payload.titleId().get();
                    if (data.hasTitle(id) || player.hasPermissions(2)) {
                        if (!data.hasTitle(id)) {
                            data.unlockTitle(id);
                        }
                        data.setActiveTitle(payload.titleId());
                        TitleNetworkHandler.syncToPlayerAndTrackers(player);
                    } else {
                        Epithet.LOGGER.warn("Player {} tried to equip unearned title: {}", player.getName().getString(), id);
                        TitleNetworkHandler.syncPlayerTitle(player, player, true);
                    }
                }
            }
        });
    }
}
