package top.atdove.epithet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import top.atdove.epithet.client.gui.PlayerTitlesManageScreen;
import top.atdove.epithet.network.SyncPlayerTitlePayload;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client data manager that coordinates title definitions, local player state,
 * and tracked player title status for overhead rendering and GUI.
 */
public class ClientTitleManager {
    private static final Map<UUID, ClientPlayerData> PLAYER_DATA = new ConcurrentHashMap<>();

    private static Optional<ResourceLocation> selfActiveTitle = Optional.empty();
    private static final Set<ResourceLocation> selfUnlockedTitles = ConcurrentHashMap.newKeySet();
    private static boolean selfLocked = false;

    public static void ensureDefaultSampleTitles() {
        // No-op: default titles are provided by datapacks or server sync
    }

    public static void handleSyncPlayerTitle(SyncPlayerTitlePayload payload) {
        PLAYER_DATA.put(payload.playerId(), new ClientPlayerData(
                payload.playerId(),
                payload.activeTitle(),
                new HashSet<>(payload.unlockedTitles()),
                payload.isLocked()
        ));

        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(payload.playerId());
        if (payload.isSelf() && isLocalPlayer) {
            selfActiveTitle = payload.activeTitle();
            selfUnlockedTitles.clear();
            selfUnlockedTitles.addAll(payload.unlockedTitles());
            selfLocked = payload.isLocked();

            // Immediately refresh client player's display name
            mc.player.refreshDisplayName();
        }

        if (mc.screen instanceof PlayerTitlesManageScreen manageScreen) {
            manageScreen.onDataSynced(payload.playerId());
        }
    }

    public static Set<ResourceLocation> getUnlockedTitles(UUID uuid) {
        ClientPlayerData data = PLAYER_DATA.get(uuid);
        Set<ResourceLocation> raw;
        if (data != null) {
            raw = data.unlockedTitles();
        } else {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getUUID().equals(uuid)) {
                raw = selfUnlockedTitles;
            } else {
                return Collections.emptySet();
            }
        }

        // Only count and return titles that still exist in the current TitleRegistry
        TitleRegistry registry = TitleRegistry.getInstance();
        Set<ResourceLocation> valid = new HashSet<>();
        for (ResourceLocation id : raw) {
            if (registry.hasTitle(id)) {
                valid.add(id);
            }
        }
        return Collections.unmodifiableSet(valid);
    }

    public static void setPlayerActiveTitle(UUID uuid, Optional<ResourceLocation> titleId) {
        ClientPlayerData existing = PLAYER_DATA.get(uuid);
        Set<ResourceLocation> unlocked = existing != null ? existing.unlockedTitles() : new HashSet<>();
        boolean locked = existing != null && existing.isLocked();
        PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, titleId, unlocked, locked));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            selfActiveTitle = titleId;
        }
    }

    public static void unlockPlayerTitle(UUID uuid, ResourceLocation titleId) {
        ClientPlayerData existing = PLAYER_DATA.get(uuid);
        Set<ResourceLocation> unlocked = existing != null ? new HashSet<>(existing.unlockedTitles()) : new HashSet<>();
        unlocked.add(titleId);
        Optional<ResourceLocation> active = existing != null ? existing.activeTitle() : Optional.empty();
        boolean locked = existing != null && existing.isLocked();
        PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, active, unlocked, locked));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            selfUnlockedTitles.add(titleId);
        }
    }

    public static void removePlayerTitle(UUID uuid, ResourceLocation titleId) {
        ClientPlayerData existing = PLAYER_DATA.get(uuid);
        if (existing != null) {
            Set<ResourceLocation> unlocked = new HashSet<>(existing.unlockedTitles());
            unlocked.remove(titleId);
            Optional<ResourceLocation> active = existing.activeTitle();
            if (active.isPresent() && active.get().equals(titleId)) {
                active = Optional.empty();
            }
            PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, active, unlocked, existing.isLocked()));
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            selfUnlockedTitles.remove(titleId);
            if (selfActiveTitle.isPresent() && selfActiveTitle.get().equals(titleId)) {
                selfActiveTitle = Optional.empty();
            }
        }
    }

    public static Optional<ResourceLocation> getLocalActiveTitle() {
        return selfActiveTitle;
    }

    public static void setLocalActiveTitle(Optional<ResourceLocation> titleId) {
        selfActiveTitle = titleId;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            UUID uuid = mc.player.getUUID();
            ClientPlayerData existing = PLAYER_DATA.get(uuid);
            Set<ResourceLocation> unlocked = existing != null ? existing.unlockedTitles() : new HashSet<>(selfUnlockedTitles);
            boolean locked = existing != null ? existing.isLocked() : selfLocked;
            PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, selfActiveTitle, unlocked, locked));
        }
    }

    public static boolean isUnlocked(ResourceLocation titleId) {
        return selfUnlockedTitles.contains(titleId);
    }

    public static boolean isSelfLocked() {
        return selfLocked;
    }

    public static void setSelfLocked(boolean locked) {
        selfLocked = locked;
    }

    public static boolean isPlayerLocked(UUID uuid) {
        ClientPlayerData data = PLAYER_DATA.get(uuid);
        return data != null && data.isLocked();
    }

    public static void togglePlayerLock(UUID uuid) {
        ClientPlayerData data = PLAYER_DATA.get(uuid);
        if (data != null) {
            PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, data.activeTitle(), data.unlockedTitles(), !data.isLocked()));
        } else {
            PLAYER_DATA.put(uuid, new ClientPlayerData(uuid, Optional.empty(), Collections.emptySet(), true));
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            selfLocked = !selfLocked;
        }
    }

    public static Optional<ResourceLocation> getActiveTitleId(UUID uuid) {
        ClientPlayerData data = PLAYER_DATA.get(uuid);
        if (data != null && data.activeTitle().isPresent()) {
            return data.activeTitle();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getUUID().equals(uuid)) {
            return selfActiveTitle;
        }
        return Optional.empty();
    }

    public static Component getActiveTitle(Player player) {
        if (player == null) return null;
        return getActiveTitle(player.getUUID());
    }

    public static Component getActiveTitle(UUID uuid) {
        Optional<ResourceLocation> activeId = getActiveTitleId(uuid);
        if (activeId.isEmpty()) {
            return null;
        }
        ResourceLocation id = activeId.get();
        Optional<TitleDefinition> defOpt = TitleRegistry.getInstance().getTitle(id);
        if (defOpt.isPresent()) {
            TitleDefinition def = defOpt.get();
            return def.getFormattedDisplayName();
        }
        return Component.literal(id.getPath());
    }

    public record ClientPlayerData(
            UUID uuid,
            Optional<ResourceLocation> activeTitle,
            Set<ResourceLocation> unlockedTitles,
            boolean isLocked
    ) {}

    public record KnownPlayerEntry(
            UUID uuid,
            String name,
            PlayerSkin skin,
            Optional<ResourceLocation> activeTitle,
            Component formattedTitle,
            boolean isLocked
    ) {}

    public static List<KnownPlayerEntry> getKnownPlayers() {
        List<KnownPlayerEntry> list = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                UUID uuid = info.getProfile().getId();
                String name = info.getProfile().getName();
                PlayerSkin skin = info.getSkin();
                Optional<ResourceLocation> titleId = getActiveTitleId(uuid);
                Component titleComp = getActiveTitle(uuid);
                boolean locked = isPlayerLocked(uuid);
                list.add(new KnownPlayerEntry(uuid, name, skin, titleId, titleComp, locked));
            }
        }
        if (list.isEmpty() && mc.player != null) {
            UUID uuid = mc.player.getUUID();
            String name = mc.player.getName().getString();
            PlayerSkin skin = DefaultPlayerSkin.get(uuid);
            Optional<ResourceLocation> titleId = selfActiveTitle;
            Component titleComp = getActiveTitle(uuid);
            list.add(new KnownPlayerEntry(uuid, name, skin, titleId, titleComp, selfLocked));
        }
        return list;
    }
}
