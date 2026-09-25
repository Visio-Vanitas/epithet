package top.atdove.epithet.attachment;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.*;

public class PlayerTitleData {
    private final Set<ResourceLocation> unlockedTitles = new HashSet<>();
    private Optional<ResourceLocation> activeTitle = Optional.empty();
    private boolean locked = false;

    public PlayerTitleData() {
    }

    public PlayerTitleData(Set<ResourceLocation> unlockedTitles, Optional<ResourceLocation> activeTitle, boolean locked) {
        this.unlockedTitles.addAll(unlockedTitles);
        this.activeTitle = activeTitle;
        this.locked = locked;
    }

    public static final Codec<PlayerTitleData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.listOf().xmap(Set::copyOf, ArrayList::new)
            .optionalFieldOf("unlockedTitles", Set.of()).forGetter(PlayerTitleData::getUnlockedTitles),
        ResourceLocation.CODEC.optionalFieldOf("activeTitle").forGetter(PlayerTitleData::getActiveTitle),
        Codec.BOOL.optionalFieldOf("locked", false).forGetter(PlayerTitleData::isLocked)
    ).apply(instance, PlayerTitleData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTitleData> STREAM_CODEC = StreamCodec.of(
        PlayerTitleData::encode,
        PlayerTitleData::decode
    );

    public static void encode(RegistryFriendlyByteBuf buf, PlayerTitleData data) {
        buf.writeVarInt(data.unlockedTitles.size());
        for (ResourceLocation id : data.unlockedTitles) {
            ResourceLocation.STREAM_CODEC.encode(buf, id);
        }
        buf.writeBoolean(data.activeTitle.isPresent());
        data.activeTitle.ifPresent(id -> ResourceLocation.STREAM_CODEC.encode(buf, id));
        buf.writeBoolean(data.locked);
    }

    public static PlayerTitleData decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Set<ResourceLocation> unlocked = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            unlocked.add(ResourceLocation.STREAM_CODEC.decode(buf));
        }
        Optional<ResourceLocation> active = buf.readBoolean()
            ? Optional.of(ResourceLocation.STREAM_CODEC.decode(buf))
            : Optional.empty();
        boolean locked = buf.readBoolean();
        return new PlayerTitleData(unlocked, active, locked);
    }

    public Set<ResourceLocation> getUnlockedTitles() {
        return unlockedTitles;
    }

    public Optional<ResourceLocation> getActiveTitle() {
        return activeTitle;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean unlockTitle(ResourceLocation titleId) {
        return unlockedTitles.add(titleId);
    }

    public boolean removeTitle(ResourceLocation titleId) {
        boolean removed = unlockedTitles.remove(titleId);
        if (activeTitle.isPresent() && activeTitle.get().equals(titleId)) {
            activeTitle = Optional.empty();
        }
        return removed;
    }

    public boolean hasTitle(ResourceLocation titleId) {
        return unlockedTitles.contains(titleId);
    }

    public void setActiveTitle(Optional<ResourceLocation> activeTitle) {
        this.activeTitle = activeTitle;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public void copyFrom(PlayerTitleData other) {
        this.unlockedTitles.clear();
        this.unlockedTitles.addAll(other.unlockedTitles);
        this.activeTitle = other.activeTitle;
        this.locked = other.locked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTitleData that = (PlayerTitleData) o;
        return locked == that.locked &&
            Objects.equals(unlockedTitles, that.unlockedTitles) &&
            Objects.equals(activeTitle, that.activeTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unlockedTitles, activeTitle, locked);
    }
}
