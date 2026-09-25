package top.atdove.epithet.platform.services;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;

public interface IPlatformHelper {
    /**
     * Gets the name of the current platform loader ("NeoForge", "Fabric", "Forge").
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given ID is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * Checks if running in development environment.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Checks if running on the physical client side.
     */
    boolean isClient();

    /**
     * Gets the configuration directory path.
     */
    Path getConfigDirectory();

    /**
     * Gets the current MinecraftServer instance if available.
     */
    MinecraftServer getCurrentServer();

    /**
     * Refreshes the display name and Tab list entry for a player across all platforms.
     */
    void refreshPlayerDisplayName(ServerPlayer player);
}
