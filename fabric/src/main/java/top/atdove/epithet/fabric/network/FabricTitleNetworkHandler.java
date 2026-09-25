package top.atdove.epithet.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import top.atdove.epithet.attachment.PlayerTitleData;
import top.atdove.epithet.fabric.duck.IPlayerTitleDataHolder;
import top.atdove.epithet.network.*;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FabricTitleNetworkHandler {

    public static void registerPayloadTypes() {
        // S2C
        PayloadTypeRegistry.playS2C().register(SyncTitleRegistryPayload.TYPE, SyncTitleRegistryPayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncPlayerTitlePayload.TYPE, SyncPlayerTitlePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(SyncDummyTitlePayload.TYPE, SyncDummyTitlePayload.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTitleGuiPayload.TYPE, OpenTitleGuiPayload.STREAM_CODEC);

        // C2S
        PayloadTypeRegistry.playC2S().register(EquipTitlePayload.TYPE, EquipTitlePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.TYPE, AdminActionPayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(OpenTitleGuiPayload.TYPE, OpenTitleGuiPayload.STREAM_CODEC);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    public static void broadcastRegistry(MinecraftServer server) {
        List<TitleDefinition> titles = new ArrayList<>(TitleRegistry.getInstance().getAllTitles().values());
        SyncTitleRegistryPayload payload = new SyncTitleRegistryPayload(titles);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            sendToPlayer(player, payload);
        }
    }

    public static void sendRegistry(ServerPlayer player) {
        List<TitleDefinition> titles = new ArrayList<>(TitleRegistry.getInstance().getAllTitles().values());
        sendToPlayer(player, new SyncTitleRegistryPayload(titles));
    }

    public static void syncPlayerTitle(ServerPlayer target, ServerPlayer recipient, boolean isSelf) {
        PlayerTitleData data = ((IPlayerTitleDataHolder) target).epithet$getTitleData();
        SyncPlayerTitlePayload payload = new SyncPlayerTitlePayload(
                target.getUUID(),
                data.getActiveTitle(),
                isSelf ? data.getUnlockedTitles() : Set.of(),
                data.isLocked(),
                isSelf
        );
        sendToPlayer(recipient, payload);
    }

    public static void syncToTrackers(ServerPlayer player) {
        PlayerTitleData data = ((IPlayerTitleDataHolder) player).epithet$getTitleData();
        SyncPlayerTitlePayload trackerPayload = new SyncPlayerTitlePayload(
                player.getUUID(),
                data.getActiveTitle(),
                Set.of(),
                data.isLocked(),
                false
        );
        for (ServerPlayer tracker : PlayerLookup.tracking(player)) {
            sendToPlayer(tracker, trackerPayload);
        }
    }

    public static void syncToPlayerAndTrackers(ServerPlayer player) {
        player.refreshDisplayName();
        player.refreshTabListName();
        syncPlayerTitle(player, player, true);
        syncToTrackers(player);
        if (player.server != null) {
            player.server.getPlayerList().broadcastAll(
                    new ClientboundPlayerInfoUpdatePacket(
                            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                            player
                    )
            );
        }
    }
}
