package top.atdove.epithet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import top.atdove.epithet.network.SyncDummyTitlePayload;
import top.atdove.epithet.test.DummyManager;
import top.atdove.epithet.title.TitleDefinition;
import top.atdove.epithet.title.TitleRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClientDummyManager {
    private static final Map<UUID, Optional<ResourceLocation>> DUMMY_TITLES = new ConcurrentHashMap<>();
    private static final Map<Integer, Optional<ResourceLocation>> DUMMY_TITLES_BY_ID = new ConcurrentHashMap<>();
    private static final Set<UUID> DUMMY_UUIDS = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> DUMMY_IDS = ConcurrentHashMap.newKeySet();

    public static void handleSync(SyncDummyTitlePayload payload) {
        if (payload.isDummy()) {
            DUMMY_TITLES.put(payload.entityUuid(), payload.titleId());
            DUMMY_TITLES_BY_ID.put(payload.entityId(), payload.titleId());
            DUMMY_UUIDS.add(payload.entityUuid());
            DUMMY_IDS.add(payload.entityId());

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity entity = mc.level.getEntity(payload.entityId());
                if (entity != null) {
                    entity.addTag(DummyManager.DUMMY_TAG);
                }
            }
        } else {
            DUMMY_TITLES.remove(payload.entityUuid());
            DUMMY_TITLES_BY_ID.remove(payload.entityId());
            DUMMY_UUIDS.remove(payload.entityUuid());
            DUMMY_IDS.remove(payload.entityId());
        }
    }

    public static boolean isDummy(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getTags().contains(DummyManager.DUMMY_TAG)) {
            return true;
        }
        if (DUMMY_UUIDS.contains(entity.getUUID())) {
            return true;
        }
        return DUMMY_IDS.contains(entity.getId());
    }

    public static Component getDummyTitle(Entity entity) {
        if (entity == null) {
            return null;
        }

        Optional<ResourceLocation> titleOpt = DUMMY_TITLES.get(entity.getUUID());
        if (titleOpt == null) {
            titleOpt = DUMMY_TITLES_BY_ID.get(entity.getId());
        }

        ResourceLocation id = null;
        if (titleOpt != null && titleOpt.isPresent()) {
            id = titleOpt.get();
        } else {
            List<TitleDefinition> defaults = TitleRegistry.getInstance().getDefaults();
            if (!defaults.isEmpty()) {
                id = defaults.get(0).id();
            }
        }

        if (id == null) {
            return null;
        }

        Optional<TitleDefinition> defOpt = TitleRegistry.getInstance().getTitle(id);
        if (defOpt.isPresent()) {
            return Component.literal("[").append(defOpt.get().displayName()).append("]");
        }
        return Component.literal("[" + id.getPath() + "]");
    }
}
