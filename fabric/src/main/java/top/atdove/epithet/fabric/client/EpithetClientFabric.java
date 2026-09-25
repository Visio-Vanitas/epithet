package top.atdove.epithet.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import top.atdove.epithet.fabric.client.gui.FabricPauseScreenHandler;
import top.atdove.epithet.fabric.network.FabricTitleNetworkHandler;

public class EpithetClientFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 1. 客户端网络监听
        FabricTitleNetworkHandler.registerPayloadTypes();

        // 2. 界面注入绑定
        FabricPauseScreenHandler.init();
    }
}
