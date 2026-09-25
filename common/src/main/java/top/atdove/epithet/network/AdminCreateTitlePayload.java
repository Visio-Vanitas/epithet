package top.atdove.epithet.network;

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
import top.atdove.epithet.data.TitleSavedData;
import top.atdove.epithet.title.TitleDefinition;

import java.util.Optional;

public record AdminCreateTitlePayload(
        TitleDefinition title,
        boolean autoEquipSelf
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AdminCreateTitlePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "admin_create_title"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminCreateTitlePayload> STREAM_CODEC = StreamCodec.composite(
            TitleDefinition.STREAM_CODEC, AdminCreateTitlePayload::title,
            ByteBufCodecs.BOOL, AdminCreateTitlePayload::autoEquipSelf,
            AdminCreateTitlePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AdminCreateTitlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.hasPermissions(2)) {
                TitleDefinition newTitle = payload.title();
                // 1. 持久化至世界数据
                TitleSavedData.get(player.server).addTitle(newTitle);
                // 2. 全服广播注册表
                TitleNetworkHandler.broadcastRegistry(player.server);

                // 3. 自动为创建者赋予并激活
                if (payload.autoEquipSelf()) {
                    PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
                    data.unlockTitle(newTitle.id());
                    data.setActiveTitle(Optional.of(newTitle.id()));
                    TitleNetworkHandler.syncToPlayerAndTrackers(player);
                }
            }
        });
    }
}
