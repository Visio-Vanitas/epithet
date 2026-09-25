package top.atdove.epithet.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import top.atdove.epithet.Epithet;
import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;
import top.atdove.epithet.client.ClientHooks;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TitleNetworkHandler {
    public static final ResourceLocation CHANNEL_ID =
        ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "main");

    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Epithet.MOD_ID).versioned("1");

        // S2C: Sync Title Registry
        registrar.playToClient(
            SyncTitleRegistryPayload.TYPE,
            SyncTitleRegistryPayload.STREAM_CODEC,
            SyncTitleRegistryPayload::handle
        );

        // S2C: Sync Player Title Data
        registrar.playToClient(
            SyncPlayerTitlePayload.TYPE,
            SyncPlayerTitlePayload.STREAM_CODEC,
            SyncPlayerTitlePayload::handle
        );

        // S2C: Sync Test Dummy Title Data
        registrar.playToClient(
            SyncDummyTitlePayload.TYPE,
            SyncDummyTitlePayload.STREAM_CODEC,
            SyncDummyTitlePayload::handle
        );

        // C2S: Equip / Unequip Title
        registrar.playToServer(
            EquipTitlePayload.TYPE,
            EquipTitlePayload.STREAM_CODEC,
            EquipTitlePayload::handle
        );

        // C2S: Admin Action
        registrar.playToServer(
            AdminActionPayload.TYPE,
            AdminActionPayload.STREAM_CODEC,
            AdminActionPayload::handle
        );

        // Bidirectional: Open Title GUI
        registrar.playBidirectional(
            OpenTitleGuiPayload.TYPE,
            OpenTitleGuiPayload.STREAM_CODEC,
            OpenTitleGuiPayload::handle
        );
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendRegistry(ServerPlayer player) {
        List<TitleDefinition> titles = new ArrayList<>(TitleRegistry.getInstance().getAllTitles().values());
        sendToPlayer(player, new SyncTitleRegistryPayload(titles));
    }

    public static void broadcastRegistry(MinecraftServer server) {
        List<TitleDefinition> titles = new ArrayList<>(TitleRegistry.getInstance().getAllTitles().values());
        PacketDistributor.sendToAllPlayers(new SyncTitleRegistryPayload(titles));
    }

    public static void syncPlayerTitle(ServerPlayer target, ServerPlayer recipient, boolean isSelf) {
        PlayerTitleData data = target.getData(ModAttachments.PLAYER_TITLE_DATA);
        SyncPlayerTitlePayload payload;
        if (isSelf) {
            payload = new SyncPlayerTitlePayload(
                target.getUUID(),
                data.getActiveTitle(),
                data.getUnlockedTitles(),
                data.isLocked(),
                true
            );
        } else {
            payload = new SyncPlayerTitlePayload(
                target.getUUID(),
                data.getActiveTitle(),
                Set.of(),
                data.isLocked(),
                false
            );
        }
        sendToPlayer(recipient, payload);
    }

    public static void syncToTrackers(ServerPlayer player) {
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
        SyncPlayerTitlePayload trackerPayload = new SyncPlayerTitlePayload(
            player.getUUID(),
            data.getActiveTitle(),
            Set.of(),
            data.isLocked(),
            false
        );
        PacketDistributor.sendToPlayersTrackingEntity(player, trackerPayload);
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

    public static void openClientGui() {
        if (FMLEnvironment.dist.isClient()) {
            ClientHooks.openTitleSelectionScreen();
        }
    }
}
