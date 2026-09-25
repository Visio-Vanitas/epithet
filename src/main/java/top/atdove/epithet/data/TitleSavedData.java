package top.atdove.epithet.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import com.mojang.logging.LogUtils;

import org.slf4j.Logger;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TitleSavedData extends SavedData {
    public static final String DATA_NAME = "epithet_dynamic_titles";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<ResourceLocation, TitleDefinition> dynamicTitles = new HashMap<>();

    public TitleSavedData() {
    }

    public static SavedData.Factory<TitleSavedData> factory() {
        return new SavedData.Factory<>(
            TitleSavedData::new,
            TitleSavedData::load,
            null
        );
    }

    public static TitleSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage storage = overworld.getDataStorage();
        return storage.computeIfAbsent(factory(), DATA_NAME);
    }

    public static TitleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TitleSavedData data = new TitleSavedData();
        ListTag listTag = tag.getList("DynamicTitles", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag titleTag = listTag.getCompound(i);
            TitleDefinition.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), titleTag)
                .resultOrPartial(error -> LOGGER.error("Failed to parse dynamic title: {}", error))
                .ifPresent(title -> data.dynamicTitles.put(title.id(), title));
        }
        TitleRegistry.getInstance().setDynamicTitles(data.dynamicTitles);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (TitleDefinition title : dynamicTitles.values()) {
            TitleDefinition.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), title)
                .resultOrPartial(error -> LOGGER.error("Failed to save dynamic title {}: {}", title.id(), error))
                .ifPresent(listTag::add);
        }
        tag.put("DynamicTitles", listTag);
        return tag;
    }

    public void addTitle(TitleDefinition title) {
        dynamicTitles.put(title.id(), title);
        TitleRegistry.getInstance().registerDynamicTitle(title);
        setDirty();
    }

    public void removeTitle(ResourceLocation id) {
        if (dynamicTitles.remove(id) != null) {
            TitleRegistry.getInstance().removeDynamicTitle(id);
            setDirty();
        }
    }

    public Map<ResourceLocation, TitleDefinition> getDynamicTitles() {
        return Collections.unmodifiableMap(dynamicTitles);
    }
}
