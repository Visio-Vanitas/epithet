package top.atdove.epithet.title;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import top.atdove.epithet.attachment.ModAttachments;
import top.atdove.epithet.attachment.PlayerTitleData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TitleRegistry {
    private static final TitleRegistry INSTANCE = new TitleRegistry();

    private final Map<ResourceLocation, TitleDefinition> datapackTitles = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, TitleDefinition> dynamicTitles = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, TitleDefinition> mergedTitles = new ConcurrentHashMap<>();

    private TitleRegistry() {}

    public static TitleRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void setDatapackTitles(Map<ResourceLocation, TitleDefinition> titles) {
        this.datapackTitles.clear();
        if (titles != null) this.datapackTitles.putAll(titles);
        rebuildMerged();
    }

    public synchronized void setDynamicTitles(Map<ResourceLocation, TitleDefinition> titles) {
        this.dynamicTitles.clear();
        if (titles != null) this.dynamicTitles.putAll(titles);
        rebuildMerged();
    }

    public synchronized void registerDynamicTitle(TitleDefinition title) {
        this.dynamicTitles.put(title.id(), title);
        rebuildMerged();
    }

    public synchronized void removeDynamicTitle(ResourceLocation id) {
        this.dynamicTitles.remove(id);
        rebuildMerged();
    }

    public synchronized void clearDynamicTitles() {
        this.dynamicTitles.clear();
        rebuildMerged();
    }

    public synchronized void setClientTitles(Collection<TitleDefinition> titles) {
        this.datapackTitles.clear();
        this.dynamicTitles.clear();
        this.mergedTitles.clear();
        if (titles != null) {
            for (TitleDefinition title : titles) {
                if (title != null && title.id() != null) {
                    this.mergedTitles.put(title.id(), title);
                }
            }
        }
    }

    private void rebuildMerged() {
        this.mergedTitles.clear();
        this.mergedTitles.putAll(this.datapackTitles);
        this.mergedTitles.putAll(this.dynamicTitles);
    }

    public Optional<TitleDefinition> getTitle(ResourceLocation id) {
        return Optional.ofNullable(mergedTitles.get(id));
    }

    public Map<ResourceLocation, TitleDefinition> getAllTitles() {
        return Collections.unmodifiableMap(mergedTitles);
    }

    public List<TitleDefinition> getDefaults() {
        return mergedTitles.values().stream()
            .filter(TitleDefinition::defaultUnlocked)
            .toList();
    }

    public Map<ResourceLocation, TitleDefinition> getDatapackTitles() {
        return Collections.unmodifiableMap(datapackTitles);
    }

    public Map<ResourceLocation, TitleDefinition> getDynamicTitles() {
        return Collections.unmodifiableMap(dynamicTitles);
    }

    public boolean hasTitle(ResourceLocation id) {
        return mergedTitles.containsKey(id);
    }

    public void clearAll() {
        this.datapackTitles.clear();
        this.dynamicTitles.clear();
        this.mergedTitles.clear();
    }

    /**
     * Prunes orphaned/deleted titles from a player's data.
     * If a title was removed from registry/datapacks, it is automatically removed
     * from player's unlocked set and unequipped if currently active.
     */
    public static boolean pruneOrphanedTitles(ServerPlayer player) {
        if (player == null) return false;
        PlayerTitleData data = player.getData(ModAttachments.PLAYER_TITLE_DATA);
        Set<ResourceLocation> unlocked = data.getUnlockedTitles();
        TitleRegistry registry = TitleRegistry.getInstance();

        Set<ResourceLocation> toRemove = new HashSet<>();
        for (ResourceLocation id : unlocked) {
            if (!registry.hasTitle(id)) {
                toRemove.add(id);
            }
        }

        boolean modified = false;
        for (ResourceLocation invalidId : toRemove) {
            data.removeTitle(invalidId);
            modified = true;
        }

        if (data.getActiveTitle().isPresent() && !registry.hasTitle(data.getActiveTitle().get())) {
            data.setActiveTitle(Optional.empty());
            modified = true;
        }

        if (modified) {
            player.refreshDisplayName();
            player.refreshTabListName();
        }
        return modified;
    }
}
