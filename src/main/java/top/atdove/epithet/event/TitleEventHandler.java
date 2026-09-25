package top.atdove.epithet.event;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;
import top.atdove.epithet.config.EpithetConfig;
import top.atdove.epithet.data.TitleSavedData;
import top.atdove.epithet.network.TitleNetworkHandler;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.List;
import java.util.Optional;

public class TitleEventHandler {

    /**
     * Unlocks titles when the player earns a corresponding advancement (similar to recipe book mechanics).
     */
    @SubscribeEvent
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResourceLocation earnedAdvId = event.getAdvancement().id();
            PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);

            for (TitleDefinition titleDef : TitleRegistry.getInstance().getAllTitles().values()) {
                if (titleDef.advancement().isPresent() && titleDef.advancement().get().equals(earnedAdvId)) {
                    if (!data.hasTitle(titleDef.id())) {
                        data.unlockTitle(titleDef.id());
                        TitleNetworkHandler.syncPlayerTitle(player, player, true);

                        Component advName = event.getAdvancement().value().name().orElse(Component.literal(earnedAdvId.toString()));
                        player.sendSystemMessage(Component.translatable(
                                "message.epithet.unlock_by_advancement",
                                advName,
                                EpithetConfig.formatTitle(titleDef.getFormattedDisplayName())
                        ).withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }
    }

    /**
     * Injects the player's active title into their display name.
     * This automatically prefixes the title in chat messages (<[Title] Player> Hello!),
     * death messages, command feedback, and other vanilla systems.
     */
    @SubscribeEvent
    public static void onPlayerNameFormat(PlayerEvent.NameFormat event) {
        Player player = event.getEntity();
        if (player != null && player.hasData(ModAttachments.PLAYER_TITLE_DATA)) {
            PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.getActiveTitle().ifPresent(titleId -> {
                Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(titleId);
                if (titleOpt.isPresent()) {
                    Component titleComp = EpithetConfig.formatChatTitle(titleOpt.get());
                    event.setDisplayname(Component.empty().append(titleComp).append(" ").append(event.getDisplayname()));
                }
            });
        }
    }

    /**
     * Injects the player's active title into their Tab list display name.
     */
    @SubscribeEvent
    public static void onTabListNameFormat(PlayerEvent.TabListNameFormat event) {
        Player player = event.getEntity();
        if (player != null && player.hasData(ModAttachments.PLAYER_TITLE_DATA)) {
            PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
            data.getActiveTitle().ifPresent(titleId -> {
                Optional<TitleDefinition> titleOpt = TitleRegistry.getInstance().getTitle(titleId);
                if (titleOpt.isPresent()) {
                    Component titleComp = EpithetConfig.formatTitle(titleOpt.get().displayName());
                    Component current = event.getDisplayName() != null ? event.getDisplayName() : player.getName();
                    event.setDisplayName(Component.empty().append(titleComp).append(" ").append(current));
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);

            // 1. Built-in default title assignment (configurable in epithet-common.toml)
            if (EpithetConfig.COMMON.enableDefaultTitle.get()) {
                TitleDefinition defaultTitle = EpithetConfig.getBuiltinDefaultTitle();
                TitleRegistry.getInstance().registerDynamicTitle(defaultTitle);
                if (!data.hasTitle(defaultTitle.id())) {
                    data.unlockTitle(defaultTitle.id());
                }
                if (EpithetConfig.COMMON.autoEquipDefaultTitle.get() && data.getActiveTitle().isEmpty()) {
                    data.setActiveTitle(Optional.of(defaultTitle.id()));
                }
            }

            // 2. Check and unlock any datapack default titles
            List<TitleDefinition> defaults = TitleRegistry.getInstance().getDefaults();
            for (TitleDefinition defTitle : defaults) {
                data.unlockTitle(defTitle.id());
            }

            // Catch-up: unlock titles if player already completed the required advancement
            for (TitleDefinition titleDef : TitleRegistry.getInstance().getAllTitles().values()) {
                if (titleDef.advancement().isPresent() && !data.hasTitle(titleDef.id())) {
                    ResourceLocation advId = titleDef.advancement().get();
                    AdvancementHolder holder = player.server.getAdvancements().get(advId);
                    if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) {
                        data.unlockTitle(titleDef.id());
                    }
                }
            }

            // Sync full title registry to the connecting player
            TitleNetworkHandler.sendRegistry(player);

            // Sync the player's own title data
            TitleNetworkHandler.syncPlayerTitle(player, player, true);

            // Refresh display name & tab list name cache for chat and achievements
            player.refreshDisplayName();
            player.refreshTabListName();

            // Broadcast tab list display name update to all players
            if (player.server != null) {
                player.server.getPlayerList().broadcastAll(
                        new ClientboundPlayerInfoUpdatePacket(
                                ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                                player
                        )
                );
            }

            // Sync to any existing trackers
            TitleNetworkHandler.syncToTrackers(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Resync title registry
            TitleNetworkHandler.sendRegistry(player);

            // Resync player title data to self and trackers
            TitleNetworkHandler.syncToPlayerAndTrackers(player);

            // Refresh display name & tab list name cache for chat and achievements
            player.refreshDisplayName();
            player.refreshTabListName();
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Resync player title data to self and trackers in the new dimension
            TitleNetworkHandler.syncToPlayerAndTrackers(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer tracker && event.getTarget() instanceof ServerPlayer target) {
            // Tracker needs to know target's active title for overhead nameplate rendering
            TitleNetworkHandler.syncPlayerTitle(target, tracker, false);
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            TitleNetworkHandler.sendRegistry(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()) {
                TitleNetworkHandler.sendRegistry(player);
            }
        }
    }

    /**
     * Initializes and loads dynamic world titles immediately after the server has started,
     * ensuring persistent titles are retained across server restarts.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TitleSavedData.get(event.getServer());
    }
}
