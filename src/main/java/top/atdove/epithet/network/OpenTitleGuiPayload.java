package top.atdove.epithet.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import top.atdove.epithet.Epithet;

public record OpenTitleGuiPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenTitleGuiPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "open_title_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTitleGuiPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {},
        buf -> new OpenTitleGuiPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenTitleGuiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                // Client side: open GUI (can be hooked by client screen handler)
                TitleNetworkHandler.openClientGui();
            } else {
                // Server side: if client requested to open GUI via C2S
                if (context.player() instanceof ServerPlayer player) {
                    TitleNetworkHandler.syncPlayerTitle(player, player, true);
                    TitleNetworkHandler.sendToPlayer(player, new OpenTitleGuiPayload());
                }
            }
        });
    }
}
