package top.atdove.epithet.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import top.atdove.epithet.Epithet;
import top.atdove.epithet.command.TitleCommands;
import top.atdove.epithet.data.TitleReloadListener;
import top.atdove.epithet.fabric.network.FabricTitleNetworkHandler;

public class EpithetFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Epithet.LOGGER.info("Initializing Epithet for Fabric 1.21.1...");

        // 1. 注册网络通信协议
        FabricTitleNetworkHandler.registerPayloadTypes();

        // 2. 指令树绑定
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TitleCommands.register(dispatcher);
        });

        // 3. 数据包重载监听器绑定
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public ResourceLocation getFabricId() {
                        return ResourceLocation.fromNamespaceAndPath(Epithet.MOD_ID, "titles");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        TitleReloadListener.reload(resourceManager);
                    }
                }
        );

        // 4. 玩家登录同步称号
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FabricTitleNetworkHandler.sendRegistry(handler.getPlayer());
            FabricTitleNetworkHandler.syncPlayerTitle(handler.getPlayer(), handler.getPlayer(), true);
        });
    }
}
