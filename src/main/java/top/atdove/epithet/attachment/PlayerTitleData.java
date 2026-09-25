package top.atdove.epithet.attachment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe player title data storage.
 * Uses HashSet in Codec to prevent duplicate elements from throwing IllegalArgumentException.
 */
public class PlayerTitleData {
    private final Set<ResourceLocation> unlockedTitles = ConcurrentHashMap.newKeySet();
    private volatile Optional<ResourceLocation> activeTitle = Optional.empty();
    private volatile boolean locked = false;

    public PlayerTitleData() {
    }

    public PlayerTitleData(Collection<ResourceLocation> unlockedTitles, Optional<ResourceLocation> activeTitle, boolean locked) {
        if (unlockedTitles != null) {
            this.unlockedTitles.addAll(unlockedTitles);
        }
        this.activeTitle = activeTitle != null ? activeTitle : Optional.empty();
        this.locked = locked;
    }

    public static final Codec<PlayerTitleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.listOf().xmap(HashSet::new, ArrayList::new)
            .optionalFieldOf("unlockedTitles", new HashSet<>()).forGetter(data -> new HashSet<>(data.getUnlockedTitles())),
        ResourceLocation.CODEC.optionalFieldOf("activeTitle").forGetter(PlayerTitleData::getActiveTitle),
        Codec.BOOL.optionalFieldOf("locked", false).forGetter(PlayerTitleData::isLocked)
    ).apply(instance, PlayerTitleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTitleData> STREAM_CODEC = StreamCodec.of(
        PlayerTitleData::encode,
        PlayerTitleData::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, PlayerTitleData data) {
        Set<ResourceLocation> copy = new HashSet<>(data.unlockedTitles);
        buf.writeVarInt(copy.size());
        for (ResourceLocation id : copy) {
            ResourceLocation.STREAM_CODEC.encode(buf, id);
        }
        buf.writeBoolean(data.activeTitle.isPresent());
        data.activeTitle.ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
        buf.writeBoolean(data.locked);
    }

    public static PlayerTitleData decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        // Defensive check against malformed packets
        int safeCount = Math.max(0, Math.min(count, 4096));
        Set<ResourceLocation> unlocked = new HashSet<>(safeCount);
        for (int i = 0; i < safeCount; i++) {
            unlocked.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }
        boolean hasActive = buf.readBoolean();
        Optional<ResourceLocation> active = hasActive
            ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
            : Optional.empty();
        boolean locked = buf.readBoolean();
        return new PlayerTitleData(unlocked, active, locked);
    }

    public Set<ResourceLocation> getUnlockedTitles() {
        return Collections.unmodifiableSet(this.unlockedTitles);
    }

    public Optional<ResourceLocation> getActiveTitle() {
        return this.activeTitle;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public synchronized boolean unlockTitle(ResourceLocation titleId) {
        if (titleId == null) return false;
        return this.unlockedTitles.add(titleId);
    }

    public synchronized boolean removeTitle(ResourceLocation titleId) {
        if (titleId == null) return false;
        boolean removed = this.unlockedTitles.remove(titleId);
        if (this.activeTitle.isPresent() && this.activeTitle.get().equals(titleId)) {
            this.activeTitle = Optional.empty();
        }
        return removed;
    }

    public boolean hasTitle(ResourceLocation titleId) {
        if (titleId == null) return false;
        return this.unlockedTitles.contains(titleId);
    }

    public synchronized void setActiveTitle(Optional<ResourceLocation> activeTitle) {
        this.activeTitle = activeTitle != null ? activeTitle : Optional.empty();
    }

    public synchronized void setLocked(boolean locked) {
        this.locked = locked;
    }

    public synchronized void copyFrom(PlayerTitleData other) {
        if (other == null) return;
        this.unlockedTitles.clear();
        this.unlockedTitles.addAll(other.unlockedTitles);
        this.activeTitle = other.activeTitle;
        this.locked = other.locked;
    }

    public PlayerTitleData copy() {
        return new PlayerTitleData(this.unlockedTitles, this.activeTitle, this.locked);
    }
}
