package top.atdove.epithet.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;

import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import org.slf4j.Logger;
import top.atdove.epithet.network.TitleNetworkHandler;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.HashMap;
import java.util.Map;

public class TitleReloadListener extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public TitleReloadListener() {
        super(GSON, "titles");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, TitleDefinition> parsedTitles = new HashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileLocation = entry.getKey();
            JsonElement json = entry.getValue();

            try {
                if (json.isJsonObject() && !json.getAsJsonObject().has("id")) {
                    json.getAsJsonObject().addProperty("id", fileLocation.toString());
                }

                TitleDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("Error parsing title {}: {}", fileLocation, error))
                    .ifPresent(title -> {
                        parsedTitles.put(title.id(), title);
                        LOGGER.debug("Loaded title: {}", title.id());
                    });
            } catch (Exception e) {
                LOGGER.error("Failed to load title from {}: {}", fileLocation, e.getMessage());
            }
        }

        LOGGER.info("Loaded {} datapack titles", parsedTitles.size());
        TitleRegistry.getInstance().setDatapackTitles(parsedTitles);

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                TitleSavedData savedData = TitleSavedData.get(server);
                TitleRegistry.getInstance().setDynamicTitles(savedData.getDynamicTitles());
                TitleNetworkHandler.broadcastRegistry(server);
            });
        }
    }

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new TitleReloadListener());
    }
}
