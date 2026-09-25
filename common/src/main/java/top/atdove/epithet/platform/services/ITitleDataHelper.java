package top.atdove.epithet.platform.services;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import top.atdove.epithet.attachment.PlayerTitleData;

public interface ITitleDataHelper {
    /**
     * Gets or initializes the title data for the specified player entity.
     */
    PlayerTitleData getPlayerData(Player player);

    /**
     * Sets or updates the title data for the specified player entity.
     */
    void setPlayerData(Player player, PlayerTitleData data);

    /**
     * Handles data preservation across player respawns or dimension changes (copyOnDeath).
     */
    void copyDataOnRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean wonGame);
}
