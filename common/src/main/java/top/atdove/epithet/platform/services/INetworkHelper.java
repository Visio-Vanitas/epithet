package top.atdove.epithet.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public interface INetworkHelper {
    /**
     * S2C: Sends a payload to a specific player.
     */
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /**
     * S2C: Sends a payload to all players tracking a specific entity.
     */
    void sendToTracking(Entity entity, CustomPacketPayload payload);

    /**
     * S2C: Sends a payload to all players tracking a specific entity, plus the entity itself if it's a player.
     */
    void sendToTrackingAndSelf(ServerPlayer player, CustomPacketPayload payload);

    /**
     * S2C: Broadcasts a payload to all online players on the server.
     */
    void sendToAll(CustomPacketPayload payload);

    /**
     * C2S: Sends a payload from the client to the dedicated/integrated server.
     */
    void sendToServer(CustomPacketPayload payload);
}
