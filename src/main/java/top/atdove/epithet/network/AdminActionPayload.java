package top.atdove.epithet.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;

import java.util.Optional;
import java.util.UUID;

public record AdminActionPayload(
        UUID targetPlayerId,
        String action,
        String param
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AdminActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "admin_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, AdminActionPayload::targetPlayerId,
            ByteBufCodecs.STRING_UTF8, AdminActionPayload::action,
            ByteBufCodecs.STRING_UTF8, AdminActionPayload::param,
            AdminActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdminActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                ServerPlayer target = player.server.getPlayerList().getPlayer(payload.targetPlayerId());
                if (target != null) {
                    PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
                    String action = payload.action();
                    boolean modified = false;

                    if ("toggle_lock".equals(action)) {
                        data.setLocked(!data.isLocked());
                        modified = true;
                    } else if ("give".equals(action)) {
                        ResourceLocation titleId = ResourceLocation.tryParse(payload.param());
                        if (titleId != null) {
                            data.unlockTitle(titleId);
                            modified = true;
                        }
                    } else if ("take".equals(action)) {
                        ResourceLocation titleId = ResourceLocation.tryParse(payload.param());
                        if (titleId != null) {
                            data.removeTitle(titleId);
                            modified = true;
                        }
                    } else if ("equip".equals(action)) {
                        ResourceLocation titleId = ResourceLocation.tryParse(payload.param());
                        if (titleId != null) {
                            data.unlockTitle(titleId);
                            data.setActiveTitle(Optional.of(titleId));
                            modified = true;
                        }
                    } else if ("unequip".equals(action)) {
                        data.setActiveTitle(Optional.empty());
                        modified = true;
                    } else if ("sync".equals(action) || "query".equals(action)) {
                        TitleNetworkHandler.syncPlayerTitle(target, player, true);
                    }

                    if (modified) {
                        TitleNetworkHandler.syncPlayerTitle(target, target, true);
                        TitleNetworkHandler.syncToTrackers(target);
                        if (!player.getUUID().equals(target.getUUID())) {
                            TitleNetworkHandler.syncPlayerTitle(target, player, true);
                        }
                    }
                }
            }
        });
    }
}
