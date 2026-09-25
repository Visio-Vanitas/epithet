package top.atdove.epithet.test;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.network.PacketDistributor;

import top.atdove.epithet.network.SyncDummyTitlePayload;
import top.atdove.epithet.network.TitleNetworkHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DummyManager {
    public static final String DUMMY_TAG = "epithet_dummy";
    public static final String DEFAULT_NAME = "TestDummy";
    public static final String NBT_TITLE_KEY = "EpithetTitle";

    private static final Map<UUID, ResourceLocation> SERVER_DUMMY_TITLES = new ConcurrentHashMap<>();

    public static boolean isDummy(Entity entity) {
        return entity != null && entity.getTags().contains(DUMMY_TAG);
    }

    public static void setDummyTitle(Entity entity, ResourceLocation titleId) {
        if (entity == null) {
            return;
        }
        if (titleId != null) {
            SERVER_DUMMY_TITLES.put(entity.getUUID(), titleId);
            entity.getPersistentData().putString(NBT_TITLE_KEY, titleId.toString());
        } else {
            SERVER_DUMMY_TITLES.remove(entity.getUUID());
            entity.getPersistentData().remove(NBT_TITLE_KEY);
        }
    }

    public static ResourceLocation getDummyTitleId(Entity entity) {
        if (entity == null) {
            return null;
        }
        ResourceLocation id = SERVER_DUMMY_TITLES.get(entity.getUUID());
        if (id == null && entity.getPersistentData().contains(NBT_TITLE_KEY)) {
            String raw = entity.getPersistentData().getString(NBT_TITLE_KEY);
            if (!raw.isEmpty()) {
                id = ResourceLocation.tryParse(raw);
                if (id != null) {
                    SERVER_DUMMY_TITLES.put(entity.getUUID(), id);
                }
            }
        }
        return id;
    }

    public static void syncDummyToTrackers(Entity entity) {
        if (entity == null) {
            return;
        }
        ResourceLocation titleId = getDummyTitleId(entity);
        SyncDummyTitlePayload payload = new SyncDummyTitlePayload(
            entity.getId(),
            entity.getUUID(),
            Optional.ofNullable(titleId),
            true
        );
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
    }

    public static void syncDummyToPlayer(Entity entity, ServerPlayer player) {
        if (entity == null || player == null) {
            return;
        }
        ResourceLocation titleId = getDummyTitleId(entity);
        SyncDummyTitlePayload payload = new SyncDummyTitlePayload(
            entity.getId(),
            entity.getUUID(),
            Optional.ofNullable(titleId),
            true
        );
        TitleNetworkHandler.sendToPlayer(player, payload);
    }

    public static void removeDummy(Entity entity) {
        if (entity == null) {
            return;
        }
        SyncDummyTitlePayload payload = new SyncDummyTitlePayload(
            entity.getId(),
            entity.getUUID(),
            Optional.empty(),
            false
        );
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload);
        SERVER_DUMMY_TITLES.remove(entity.getUUID());
        entity.discard();
    }

    public static ArmorStand findNearestDummy(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        List<ArmorStand> list = player.level().getEntitiesOfClass(
            ArmorStand.class,
            box,
            e -> e.getTags().contains(DUMMY_TAG)
        );
        return list.stream().min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
    }

    public static List<ArmorStand> findNearbyDummies(ServerPlayer player, double radius) {
        AABB box = player.getBoundingBox().inflate(radius);
        return player.level().getEntitiesOfClass(
            ArmorStand.class,
            box,
            e -> e.getTags().contains(DUMMY_TAG)
        );
    }
}
